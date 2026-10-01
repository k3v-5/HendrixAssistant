import os
import sys
import pytest
from unittest.mock import patch, MagicMock
from automation.antigravity_manager import AntigravityManager

@pytest.fixture
def mock_ag_manager(tmp_path):
    mgr = AntigravityManager()
    fake_exe = tmp_path / "Antigravity.exe"
    fake_exe.write_text("fake binary", encoding="utf-8")
    mgr.exe_path = str(fake_exe)
    mgr.db_path = str(tmp_path / "conversation_summaries.db")
    return mgr

def test_launch_or_focus_existing_window(mock_ag_manager):
    with patch.object(mock_ag_manager, 'find_antigravity_window', return_value=12345):
        with patch('ctypes.windll.user32.ShowWindow') as mock_show:
            with patch('ctypes.windll.user32.SetForegroundWindow') as mock_fg:
                res = mock_ag_manager.launch_or_focus()
                assert res is True
                mock_show.assert_called_once_with(12345, 9)
                mock_fg.assert_called_once_with(12345)

def test_launch_or_focus_spawn_process(mock_ag_manager, tmp_path):
    workspace = tmp_path / "test_workspace"
    workspace.mkdir()

    with patch.object(mock_ag_manager, 'find_antigravity_window', return_value=None):
        with patch.object(mock_ag_manager, 'wait_for_window', return_value=12345):
            with patch('subprocess.Popen') as mock_popen:
                res = mock_ag_manager.launch_or_focus(workspace_path=str(workspace))
                assert res is True
                mock_popen.assert_called_once()
                args, kwargs = mock_popen.call_args
                assert args[0] == [mock_ag_manager.exe_path, str(workspace)]
                assert "creationflags" in kwargs

def test_launch_or_focus_fallback_startfile(mock_ag_manager):
    with patch.object(mock_ag_manager, 'find_antigravity_window', return_value=None):
        with patch.object(mock_ag_manager, 'wait_for_window', return_value=12345):
            with patch('subprocess.Popen', side_effect=OSError("Popen failed")):
                with patch('os.startfile', create=True) as mock_startfile:
                    res = mock_ag_manager.launch_or_focus()
                    assert res is True
                    mock_startfile.assert_called_once_with(mock_ag_manager.exe_path)

def test_launch_or_focus_force_relaunch(mock_ag_manager):
    # force_relaunch should ignore any existing HWND and spawn a new instance
    with patch.object(mock_ag_manager, 'find_antigravity_window', return_value=999):
        with patch.object(mock_ag_manager, 'wait_for_window', return_value=12345):
            with patch('os.startfile', create=True) as mock_startfile:
                res = mock_ag_manager.launch_or_focus(force_relaunch=True)
                assert res is True
                mock_startfile.assert_called_once_with(mock_ag_manager.exe_path)

def test_execute_action_existing_chat(mock_ag_manager):
    with patch.object(mock_ag_manager, 'launch_or_focus', return_value=True):
        with patch.object(mock_ag_manager, 'wait_for_window', return_value=12345):
            with patch.object(mock_ag_manager, '_inject_prompt') as mock_inject:
                with patch('os.startfile', create=True) as mock_startfile:
                    res = mock_ag_manager.execute_action(
                        mode="EXISTING_CHAT",
                        conversation_id="conv-1234",
                        prompt="Continúa",
                        force_relaunch=True
                    )
                    assert res is True
                    mock_startfile.assert_called_once_with("antigravity://conversation/conv-1234")
                    mock_inject.assert_called_once_with("Continúa")

def test_wait_for_window_success(mock_ag_manager):
    # Simulate window appearing after 1 check
    calls = [None, 99999]
    def mock_find():
        return calls.pop(0) if calls else 99999

    with patch.object(mock_ag_manager, 'find_antigravity_window', side_effect=mock_find):
        with patch('time.sleep'):
            hwnd = mock_ag_manager.wait_for_window(timeout_sec=5.0)
            assert hwnd == 99999

def test_wait_for_window_timeout(mock_ag_manager):
    with patch.object(mock_ag_manager, 'find_antigravity_window', return_value=None):
        with patch('time.sleep'):
            hwnd = mock_ag_manager.wait_for_window(timeout_sec=0.1)
            assert hwnd is None
