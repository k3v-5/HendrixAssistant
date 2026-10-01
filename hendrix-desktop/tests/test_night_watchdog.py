import os
import json
import pytest
from unittest.mock import MagicMock
from automation.night_watchdog import NightTaskWatchdog

@pytest.fixture
def mock_watchdog_env(tmp_path):
    log_file = tmp_path / "language_server.log"
    log_file.write_text("initial log content\n", encoding="utf-8")

    ws_dir = tmp_path / "workspace"
    ws_dir.mkdir()

    profile_mgr = MagicMock()
    profile_mgr.get_active_profile.return_value = "cuenta1"
    profile_mgr.get_next_available_profile.return_value = "cuenta2"
    profile_mgr.activate_profile.return_value = True

    ag_mgr = MagicMock()
    ag_mgr.execute_action.return_value = True

    watchdog = NightTaskWatchdog(
        profile_manager=profile_mgr,
        ag_manager=ag_mgr,
        log_path=str(log_file),
        poll_interval=0.1
    )

    return watchdog, profile_mgr, ag_mgr, str(log_file), str(ws_dir)

def test_start_and_stop_task(mock_watchdog_env):
    watchdog, profile_mgr, ag_mgr, log_path, ws_dir = mock_watchdog_env

    events = []
    watchdog.register_event_callback(lambda et, p: events.append(et))

    success = watchdog.start_task(
        goal="Construir sistema de autenticación",
        workspace=ws_dir,
        max_turns_per_account=30
    )
    assert success is True
    assert watchdog.running is True

    status = watchdog.get_status()
    assert status["status"] == "RUNNING"
    assert status["goal"] == "Construir sistema de autenticación"
    assert status["activeProfile"] == "cuenta1"

    # Verificar que el checkpoint fue creado en .agents/overnight_task.json
    checkpoint_file = os.path.join(ws_dir, ".agents", "overnight_task.json")
    assert os.path.exists(checkpoint_file)

    with open(checkpoint_file, "r", encoding="utf-8") as f:
        cp_data = json.load(f)
    assert cp_data["goal"] == "Construir sistema de autenticación"

    # Detener tarea
    stopped = watchdog.stop_task("USER_STOP")
    assert stopped is True
    assert watchdog.running is False
    assert watchdog.get_status()["status"] == "IDLE"

    assert "NIGHT_TASK_STARTED" in events
    assert "NIGHT_TASK_STOPPED" in events

def test_check_log_for_quota(mock_watchdog_env):
    watchdog, profile_mgr, ag_mgr, log_path, ws_dir = mock_watchdog_env

    # 1. Sin nuevas líneas
    assert watchdog.check_log_for_quota() is False

    # 2. Agregar línea normal
    with open(log_path, "a", encoding="utf-8") as f:
        f.write("Normal model inference completed successfully.\n")
    assert watchdog.check_log_for_quota() is False

    # 3. Agregar error de cuota 429
    with open(log_path, "a", encoding="utf-8") as f:
        f.write("ERROR: HTTP 429 RESOURCE_EXHAUSTED Quota exceeded for user.\n")
    assert watchdog.check_log_for_quota() is True

def test_trigger_rotation(mock_watchdog_env):
    watchdog, profile_mgr, ag_mgr, log_path, ws_dir = mock_watchdog_env
    events = []
    watchdog.register_event_callback(lambda et, p: events.append(et))

    watchdog.start_task("Refactorizar código", ws_dir, max_turns_per_account=40)

    # Disparar rotación por cuota agotada
    watchdog.trigger_rotation("QUOTA_EXHAUSTED")

    # Debe haber marcado cuenta1 en cooldown
    profile_mgr.mark_quota_exhausted.assert_called_with("cuenta1", cooldown_hours=4.0)

    # Debe haber activado cuenta2
    profile_mgr.activate_profile.assert_called_with("cuenta2", kill_running=True)

    # Debe haber inyectado prompt de continuación
    ag_mgr.execute_action.assert_called()

    status = watchdog.get_status()
    assert status["rotationsCount"] == 1
    assert status["activeProfile"] == "cuenta2"

    assert "NIGHT_TASK_ROTATING" in events
    assert "NIGHT_TASK_RESUMED" in events

    watchdog.stop_task()

def test_trigger_loop_recovery(mock_watchdog_env):
    watchdog, profile_mgr, ag_mgr, log_path, ws_dir = mock_watchdog_env
    events = []
    watchdog.register_event_callback(lambda et, p: events.append(et))

    watchdog.start_task("Crear tests", ws_dir)

    # 1era recuperación
    watchdog.trigger_loop_recovery("TOOL_LOOP")
    assert watchdog.get_status()["loopRecoveriesCount"] == 1
    assert "NIGHT_TASK_LOOP_DETECTED" in events

    # 2da recuperación
    watchdog.trigger_loop_recovery("TOOL_LOOP")
    assert watchdog.get_status()["loopRecoveriesCount"] == 2

    # 3ra recuperación
    watchdog.trigger_loop_recovery("TOOL_LOOP")
    assert watchdog.get_status()["loopRecoveriesCount"] == 3

    # 4ta recuperación (excede el máximo de 3) -> debe forzar rotación
    watchdog.trigger_loop_recovery("TOOL_LOOP")
    assert watchdog.get_status()["rotationsCount"] == 1

    watchdog.stop_task()

def test_verification_command(mock_watchdog_env):
    watchdog, profile_mgr, ag_mgr, log_path, ws_dir = mock_watchdog_env

    # Con comando exitoso
    watchdog.task_state["verificationCmd"] = "python -c \"exit(0)\""
    watchdog.task_state["workspace"] = ws_dir
    assert watchdog.run_verification() is True

    # Con comando fallido
    watchdog.task_state["verificationCmd"] = "python -c \"exit(1)\""
    assert watchdog.run_verification() is False
