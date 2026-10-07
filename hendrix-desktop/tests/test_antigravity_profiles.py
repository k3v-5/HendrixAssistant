import os
import json
import shutil
import pytest
from datetime import datetime, timezone, timedelta
from automation.antigravity_profiles import AntigravityProfileManager

@pytest.fixture
def mock_profile_env(tmp_path):
    base_profiles = tmp_path / "profiles"
    appdata = tmp_path / "appdata"

    base_profiles.mkdir(parents=True)
    appdata.mkdir(parents=True)

    # Crear estructura básica de appdata
    storage_file = appdata / "app_storage.json"
    storage_data = {
        "jetski.onboarding.lastLoginUsername": "testuser@gmail.com",
        "themeMode": "THEME_MODE_DARK"
    }
    storage_file.write_text(json.dumps(storage_data), encoding="utf-8")

    network_dir = appdata / "Network"
    network_dir.mkdir()
    (network_dir / "Cookies").write_text("dummy_cookies_content", encoding="utf-8")

    from unittest.mock import MagicMock
    mock_vault = MagicMock()
    mock_vault.read_credential.return_value = None
    mock_vault.write_credential.return_value = True

    mgr = AntigravityProfileManager(
        base_profiles_dir=str(base_profiles),
        appdata_dir=str(appdata),
        vault=mock_vault
    )
    return mgr, str(base_profiles), str(appdata)

def test_manifest_creation_and_empty_list(mock_profile_env):
    mgr, base_profiles, appdata = mock_profile_env
    manifest_path = os.path.join(base_profiles, "profiles_manifest.json")
    assert os.path.exists(manifest_path)

    profiles = mgr.list_profiles()
    assert profiles == []
    assert mgr.get_active_profile() is None

def test_capture_and_activate_profile(mock_profile_env):
    mgr, base_profiles, appdata = mock_profile_env

    # 1. Capturar perfil 1
    success = mgr.capture_current_profile("cuenta1")
    assert success is True
    assert mgr.get_active_profile() == "cuenta1"

    profiles = mgr.list_profiles()
    assert len(profiles) == 1
    assert profiles[0]["name"] == "cuenta1"
    assert profiles[0]["email"] == "testuser@gmail.com"
    assert profiles[0]["isActive"] is True
    assert profiles[0]["inCooldown"] is False

    # 2. Modificar AppData para simular otra cuenta logueada
    storage_file = os.path.join(appdata, "app_storage.json")
    with open(storage_file, "w", encoding="utf-8") as f:
        json.dump({"jetski.onboarding.lastLoginUsername": "segundacuenta@gmail.com"}, f)

    success2 = mgr.capture_current_profile("cuenta2")
    assert success2 is True
    assert mgr.get_active_profile() == "cuenta2"

    profiles = mgr.list_profiles()
    assert len(profiles) == 2

    # 3. Reactivar cuenta1
    # Sobrescribir kill_running para tests unitarios
    mgr.close_antigravity_processes = lambda timeout_sec=3.0: True
    act_success = mgr.activate_profile("cuenta1", kill_running=False)
    assert act_success is True
    assert mgr.get_active_profile() == "cuenta1"

    # Verificar que el archivo en AppData se restauró con cuenta1
    with open(storage_file, "r", encoding="utf-8") as f:
        restored_data = json.load(f)
    assert restored_data["jetski.onboarding.lastLoginUsername"] == "testuser@gmail.com"

def test_cooldown_and_next_available_profile(mock_profile_env):
    mgr, base_profiles, appdata = mock_profile_env
    mgr.close_antigravity_processes = lambda timeout_sec=3.0: True

    # Crear 3 perfiles
    mgr.capture_current_profile("acc1", email="acc1@gmail.com")
    mgr.capture_current_profile("acc2", email="acc2@gmail.com")
    mgr.capture_current_profile("acc3", email="acc3@gmail.com")

    # Actualmente 'acc3' está activa
    assert mgr.get_active_profile() == "acc3"

    # Marcar 'acc3' con cuota agotada
    mgr.mark_quota_exhausted("acc3", cooldown_hours=4.0)

    # El siguiente disponible debe ser acc1 o acc2 (no acc3)
    next_prof = mgr.get_next_available_profile(exclude_current=True)
    assert next_prof in ["acc1", "acc2"]

    # Marcar acc1 en cooldown
    mgr.mark_quota_exhausted(next_prof, cooldown_hours=4.0)

    # El que queda disponible
    remaining = mgr.get_next_available_profile(exclude_current=True)
    assert remaining is not None
    assert remaining not in ["acc3", next_prof]

    # Marcar el último en cooldown
    mgr.mark_quota_exhausted(remaining, cooldown_hours=4.0)

    # Ahora todos están en cooldown
    all_busy = mgr.get_next_available_profile(exclude_current=False)
    assert all_busy is None

def test_delete_profile(mock_profile_env):
    mgr, base_profiles, appdata = mock_profile_env
    mgr.capture_current_profile("temp_acc")
    assert mgr.get_active_profile() == "temp_acc"

    deleted = mgr.delete_profile("temp_acc")
    assert deleted is True
    assert mgr.get_active_profile() is None
    assert mgr.list_profiles() == []

def test_capture_current_with_email(mock_profile_env):
    mgr, base_profiles, appdata = mock_profile_env
    success = mgr.capture_current_profile("cuenta_custom", email="custom@gmail.com")
    assert success is True
    profiles = mgr.list_profiles()
    p = next(x for x in profiles if x["name"] == "cuenta_custom")
    assert p["email"] == "custom@gmail.com"

def test_night_runner_cli_capture_args():
    import subprocess
    import sys
    root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    night_runner_script = os.path.join(root_dir, "night_runner.py")
    res = subprocess.run([sys.executable, night_runner_script, "--help"], capture_output=True, text=True)
    assert res.returncode == 0
    assert "--capture-current" in res.stdout
    assert "--email" in res.stdout
