from config import config

class SecurityGuard:
    @staticmethod
    def can_capture_screen() -> bool:
        return config.allow_screen_capture

    @staticmethod
    def can_inject_mouse() -> bool:
        return config.allow_mouse_control

    @staticmethod
    def can_inject_keyboard() -> bool:
        return config.allow_keyboard_control

    @staticmethod
    def can_execute_commands() -> bool:
        return config.allow_terminal_commands
