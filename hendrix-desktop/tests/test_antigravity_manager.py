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
                with patch('pyautogui.sleep'):
                    res = mock_ag_manager.execute_action(
                        mode="EXISTING_CHAT",
                        conversation_id="conv-1234",
                        prompt="Continúa",
                        force_relaunch=True
                    )
                    assert res is True
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

def test_inject_prompt_safety_terminal_protection(mock_ag_manager):
    # Si la ventana activa no es Antigravity, no debe inyectar texto para no afectar la terminal
    with patch.object(mock_ag_manager, 'find_antigravity_window', return_value=12345):
        with patch.object(mock_ag_manager, 'force_foreground_window', return_value=False):
            with patch('ctypes.windll.user32.GetForegroundWindow', return_value=88888):
                with patch('psutil.Process') as mock_proc:
                    mock_proc.return_value.name.return_value = "pwsh.exe"
                    res = mock_ag_manager._inject_prompt("Continúa")
                    assert res is False

def test_get_chat_by_id(mock_ag_manager):
    import sqlite3
    # Crear tabla y datos en db_path
    conn = sqlite3.connect(mock_ag_manager.db_path)
    c = conn.cursor()
    c.execute("""
        CREATE TABLE conversation_summaries (
            conversation_id TEXT PRIMARY KEY,
            title TEXT,
            preview TEXT,
            last_modified_time TEXT,
            step_count INTEGER,
            project_id TEXT,
            workspace_uris TEXT,
            parent_conversation_id TEXT,
            killed INTEGER
        )
    """)
    c.execute("""
        INSERT INTO conversation_summaries VALUES (
            'conv-1234', 'Motor MCP Para Ableton', 'Preview text', '2026-10-01', 5, 'p1', 'file:///F:/Dev/AbletonEngine', '', 0
        )
    """)
    conn.commit()
    conn.close()

    chat = mock_ag_manager.get_chat_by_id('conv-1234')
    assert chat is not None
    assert chat['conversationId'] == 'conv-1234'
    assert chat['title'] == 'Motor MCP Para Ableton'

def test_select_conversation_matching(mock_ag_manager):
    # Simular pywinauto Desktop
    mock_link = MagicMock()
    mock_link.window_text.return_value = "Motor MCP Para Ableton"
    mock_rect = MagicMock()
    mock_rect.left = 100
    mock_rect.right = 200
    mock_rect.top = 300
    mock_rect.bottom = 350
    mock_link.rectangle.return_value = mock_rect

    with patch('pywinauto.Desktop') as mock_desktop:
        mock_app = MagicMock()
        mock_desktop.return_value.window.return_value = mock_app
        mock_app.descendants.return_value = [mock_link]
        with patch('pyautogui.click') as mock_click:
            with patch('pyautogui.sleep'):
                res = mock_ag_manager.select_conversation(
                    hwnd=12345,
                    title="Motor MCP Para Ableton"
                )
                assert res is True
                mock_click.assert_called_once_with(150, 325)

def test_get_active_conversations(mock_ag_manager):
    import sqlite3
    conn = sqlite3.connect(mock_ag_manager.db_path)
    c = conn.cursor()
    c.execute("""
        CREATE TABLE conversation_summaries (
            conversation_id TEXT PRIMARY KEY,
            title TEXT,
            preview TEXT,
            last_modified_time TEXT,
            step_count INTEGER,
            project_id TEXT,
            workspace_uris TEXT,
            parent_conversation_id TEXT,
            status TEXT,
            not_fully_idle INTEGER,
            killed INTEGER
        )
    """)
    c.execute("""
        INSERT INTO conversation_summaries VALUES (
            'conv-1', 'Chat Activo 1', 'Preview', '2026-10-01', 5, 'p1', 'file:///F:/Dev/A', '', 'CASCADE_RUN_STATUS_RUNNING', 1, 0
        )
    """)
    c.execute("""
        INSERT INTO conversation_summaries VALUES (
            'conv-2', 'Chat Activo 2', 'Preview', '2026-10-01', 10, 'p2', 'file:///F:/Dev/B', '', 'CASCADE_RUN_STATUS_IDLE', 1, 0
        )
    """)
    c.execute("""
        INSERT INTO conversation_summaries VALUES (
            'conv-3', 'Chat Inactivo', 'Preview', '2026-09-30', 2, 'p3', 'file:///F:/Dev/C', '', 'CASCADE_RUN_STATUS_IDLE', 0, 0
        )
    """)
    conn.commit()
    conn.close()

    active = mock_ag_manager.get_active_conversations()
    assert len(active) == 2
    titles = [a['title'] for a in active]
    assert 'Chat Activo 1' in titles
    assert 'Chat Activo 2' in titles

def test_resume_multiple_conversations(mock_ag_manager):
    chats = [
        {"conversationId": "c1", "title": "Chat 1", "workspaceUri": "file:///F:/Dev/A"},
        {"conversationId": "c2", "title": "Chat 2", "workspaceUri": "file:///F:/Dev/B"}
    ]
    with patch.object(mock_ag_manager, 'find_antigravity_window', return_value=12345):
        with patch.object(mock_ag_manager, 'force_foreground_window', return_value=True):
            with patch.object(mock_ag_manager, 'select_conversation', return_value=True) as mock_select:
                with patch.object(mock_ag_manager, '_inject_prompt', return_value=True) as mock_inject:
                    with patch('pyautogui.sleep'):
                        count = mock_ag_manager.resume_multiple_conversations(chats, "Continúa")
                        assert count == 2
                        assert mock_select.call_count == 2
                        assert mock_inject.call_count == 2

def test_execute_action_resume_all(mock_ag_manager):
    chats = [{"conversationId": "c1", "title": "Chat 1"}]
    with patch.object(mock_ag_manager, 'launch_or_focus', return_value=True):
        with patch.object(mock_ag_manager, 'wait_for_window', return_value=12345):
            with patch.object(mock_ag_manager, 'resume_multiple_conversations', return_value=1) as mock_resume:
                with patch('pyautogui.sleep'):
                    res = mock_ag_manager.execute_action(
                        mode="RESUME_ALL",
                        active_chats=chats,
                        prompt="Continúa",
                        force_relaunch=True
                    )
                    assert res is True
                    mock_resume.assert_called_once_with(chats=chats, prompt="Continúa")
