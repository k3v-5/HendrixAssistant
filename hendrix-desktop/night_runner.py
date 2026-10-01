import os
import sys
import argparse
import time
from automation.antigravity_profiles import antigravity_profile_manager
from automation.night_watchdog import night_task_watchdog

def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(
        description="Hendrix Antigravity Night Runner: Supervisión nocturna y rotación de cuentas Gemini Pro"
    )
    parser.add_argument("--goal", type=str, help="Meta u objetivo a ejecutar durante la noche")
    parser.add_argument("--workspace", type=str, default=None, help="Directorio raíz del proyecto/workspace (opcional)")
    parser.add_argument("--max-turns", type=int, default=40, help="Límite proactivo de turnos por cuenta antes de rotar (default: 40, 0 para desactivar)")
    parser.add_argument("--verify-cmd", type=str, default=None, help="Comando de verificación final (ej. 'pytest tests/' o './gradlew test')")

    parser.add_argument("--list-profiles", action="store_true", help="Lista todas las cuentas capturadas y su estado de cuota")
    parser.add_argument("--capture-current", type=str, metavar="NAME", help="Captura la cuenta actualmente logueada en Antigravity con un nombre dado")
    parser.add_argument("--close-running", action="store_true", help="Cierra Antigravity automáticamente antes de capturar para evitar que Network\\Cookies esté bloqueado")
    parser.add_argument("--reopen", action="store_true", help="Vuelve a abrir Antigravity automáticamente tras capturar el perfil")
    parser.add_argument("--activate", type=str, metavar="NAME", help="Activa la cuenta indicada como la sesión principal")
    parser.add_argument("--resume-chat", nargs="?", const="latest", type=str, metavar="ID", help="Reanuda la conversación indicada o la más reciente ('latest') tras activar")
    parser.add_argument("--prompt", type=str, default=None, help="Prompt a enviar automáticamente a Antigravity tras la activación")
    parser.add_argument("--no-resume", action="store_true", help="No reanudar automáticamente la conversación activa previa")
    parser.add_argument("--no-reopen", action="store_true", help="No reabrir Antigravity automáticamente tras activar un perfil")
    parser.add_argument("--mark-exhausted", type=str, metavar="NAME", help="Pone un perfil en cooldown de 4 horas por límite de cuota")

    args = parser.parse_args()

    # Gestión de Perfiles
    if args.list_profiles:
        profiles = antigravity_profile_manager.list_profiles()
        active = antigravity_profile_manager.get_active_profile()
        print("\n=== Cuentas de Gemini Pro Registradas ===")
        if not profiles:
            print("No hay perfiles capturados aún. Usa '--capture-current <nombre>' con Antigravity logueado.")
        for p in profiles:
            star = "★ [ACTIVO]" if p["isActive"] else " "
            auth = "🔑 [OAUTH OK]" if p.get("hasCredential") else "⚠️ [FALTA OAUTH]"
            cooldown = f"⛔ COOLDOWN ({p['cooldownRemainingSeconds']}s restantes)" if p["inCooldown"] else "✅ DISPONIBLE"
            print(f" {star} {p['name']} | Email: {p['email'] or 'N/A'} | {auth} | Usos: {p['usageCount']} | Estado: {cooldown}")
        print()
        return

    if args.capture_current:
        is_running = antigravity_profile_manager.is_antigravity_running()
        should_close = args.close_running

        if is_running and not should_close:
            try:
                if sys.stdin.isatty():
                    ans = input("⚠️ Antigravity está abierto (lo que bloquea el archivo de cookies). ¿Deseas cerrarlo automáticamente para capturar la sesión completa? (S/n): ")
                    if ans.strip().lower() in ["", "s", "si", "y", "yes"]:
                        should_close = True
                else:
                    print("⚠️ Antigravity está abierto en segundo plano. Para incluir las cookies de sesión, usa '--close-running'.")
            except Exception:
                pass

        print(f"Capturando sesión activa de Antigravity como perfil '{args.capture_current}'...")
        success = antigravity_profile_manager.capture_current_profile(args.capture_current, close_running=should_close)
        if success:
            print(f"✅ Perfil '{args.capture_current}' guardado con éxito.")
            if args.reopen:
                from automation.antigravity_manager import antigravity_manager
                print("Reabriendo Antigravity...")
                antigravity_manager.launch_or_focus()
        else:
            print(f"❌ No se pudo capturar el perfil '{args.capture_current}'. Asegúrate de haber iniciado sesión en Antigravity.")
        return

    if args.activate:
        from automation.antigravity_manager import antigravity_manager
        import urllib.parse

        # 1. Detectar conversación activa y workspace ANTES de cerrar Antigravity
        recent_chats = antigravity_manager.get_recent_chats(limit=1)
        active_chat = recent_chats[0] if recent_chats else None

        target_cid = None
        target_ws = os.path.abspath(args.workspace) if args.workspace and os.path.exists(args.workspace) else None

        if not args.no_resume:
            if args.resume_chat and args.resume_chat != "latest":
                target_cid = args.resume_chat
            elif active_chat:
                target_cid = active_chat["conversationId"]
                print(f"📍 Conversación activa previa detectada: '{active_chat['title']}' ({target_cid})")
                if not target_ws and active_chat.get("workspaceUri"):
                    uri = active_chat["workspaceUri"]
                    clean_ws = urllib.parse.unquote(uri.replace("file:///", "").replace("file://", ""))
                    if os.path.exists(clean_ws):
                        target_ws = clean_ws

        print(f"Activando perfil '{args.activate}'...")
        success = antigravity_profile_manager.activate_profile(args.activate, kill_running=True)
        if success:
            print(f"✅ Perfil '{args.activate}' activado exitosamente.")
            if not args.no_reopen:
                print("🚀 Reabriendo Antigravity automáticamente con la nueva cuenta...")

                continuation_prompt = args.prompt
                if not continuation_prompt and not args.no_resume and target_cid:
                    continuation_prompt = "Continúa con la tarea que estabas realizando"

                mode = "EXISTING_CHAT" if target_cid else "LAUNCH_OR_FOCUS"
                if continuation_prompt and not target_cid:
                    mode = "NEW_CHAT"

                antigravity_manager.execute_action(
                    mode=mode,
                    project_uri=target_ws,
                    conversation_id=target_cid,
                    prompt=continuation_prompt,
                    force_relaunch=True
                )
                if target_cid:
                    print(f"✅ Conversación reanudada: '{active_chat['title'] if active_chat else target_cid}' con prompt enviado.")
                else:
                    print("✅ Antigravity iniciado correctamente con la nueva sesión.")
        else:
            print(f"❌ Error al activar el perfil '{args.activate}'.")
        return

    if args.mark_exhausted:
        antigravity_profile_manager.mark_quota_exhausted(args.mark_exhausted)
        print(f"✅ Perfil '{args.mark_exhausted}' colocado en cooldown.")
        return

    # Ejecución de Tarea Nocturna
    if args.goal:
        ws = os.path.abspath(args.workspace or os.getcwd())
        print("=============================================================")
        print("🪐 HENDRIX ANTIGRAVITY NIGHT WATCHDOG")
        print(f"Objetivo: {args.goal}")
        print(f"Espacio de trabajo: {ws}")
        print(f"Turnos máx. por cuenta: {args.max_turns}")
        print(f"Comando de verificación: {args.verify_cmd or 'Ninguno'}")
        print("=============================================================")

        def on_event(event_type: str, payload: dict):
            details = payload.get("details", {})
            print(f"[{payload.get('timestamp', '')[:19]}] 🔔 EVENTO: {event_type} -> {details}")

        night_task_watchdog.register_event_callback(on_event)
        started = night_task_watchdog.start_task(
            goal=args.goal,
            workspace=ws,
            max_turns_per_account=args.max_turns,
            verification_cmd=args.verify_cmd
        )

        if not started:
            print("❌ No se pudo iniciar la tarea nocturna.")
            return

        print("Presiona Ctrl+C en cualquier momento para detener la supervisión.\n")
        try:
            while night_task_watchdog.running:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\nDeteniendo tarea nocturna a petición del usuario...")
            night_task_watchdog.stop_task("USER_KEYBOARD_INTERRUPT")
            print("✅ Tarea finalizada limpiamente.")
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
