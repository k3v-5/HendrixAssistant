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
        print(f"Activando perfil '{args.activate}'...")
        success = antigravity_profile_manager.activate_profile(args.activate)
        if success:
            print(f"✅ Perfil '{args.activate}' activado exitosamente.")
            if not args.no_reopen:
                from automation.antigravity_manager import antigravity_manager
                print("🚀 Reabriendo Antigravity automáticamente con la nueva cuenta...")
                ws = os.path.abspath(args.workspace) if args.workspace and os.path.exists(args.workspace) else None
                reopened = antigravity_manager.launch_or_focus(workspace_path=ws)
                if reopened:
                    print("✅ Antigravity iniciado correctamente con la nueva sesión.")
                else:
                    print("⚠️ No se pudo iniciar Antigravity automáticamente. Por favor ábrelo manualmente.")
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
