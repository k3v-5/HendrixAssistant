import pyautogui
from core.security_guard import SecurityGuard

pyautogui.FAILSAFE = False

class InputController:
    def __init__(self):
        self.screen_width, self.screen_height = pyautogui.size()

    def refresh_screen_size(self):
        self.screen_width, self.screen_height = pyautogui.size()

    def process_action(self, action_dict: dict) -> bool:
        action_type = action_dict.get("actionType", "")

        x_ratio = action_dict.get("xRatio")
        y_ratio = action_dict.get("yRatio")

        target_x = None
        target_y = None
        if x_ratio is not None and y_ratio is not None:
            self.refresh_screen_size()
            target_x = int(float(x_ratio) * self.screen_width)
            target_y = int(float(y_ratio) * self.screen_height)

        # Acciones de ratón
        if action_type in ["CLICK", "DOUBLE_CLICK", "RIGHT_CLICK", "MOUSE_MOVE", "SCROLL"]:
            if not SecurityGuard.can_inject_mouse():
                return False

            if action_type == "CLICK":
                if target_x is not None and target_y is not None:
                    pyautogui.click(target_x, target_y)
                else:
                    pyautogui.click()
                return True

            elif action_type == "DOUBLE_CLICK":
                if target_x is not None and target_y is not None:
                    pyautogui.doubleClick(target_x, target_y)
                else:
                    pyautogui.doubleClick()
                return True

            elif action_type == "RIGHT_CLICK":
                if target_x is not None and target_y is not None:
                    pyautogui.rightClick(target_x, target_y)
                else:
                    pyautogui.rightClick()
                return True

            elif action_type == "MOUSE_MOVE":
                if target_x is not None and target_y is not None:
                    pyautogui.moveTo(target_x, target_y, duration=0.05)
                return True

            elif action_type == "SCROLL":
                scroll_y = int(action_dict.get("scrollDeltaY", 0) * 50)
                if scroll_y != 0:
                    pyautogui.scroll(scroll_y)
                return True

        # Acciones de teclado
        if action_type in ["KEY_PRESS", "HOTKEY", "TYPE_TEXT"]:
            if not SecurityGuard.can_inject_keyboard():
                return False

            if action_type == "KEY_PRESS":
                key_codes = action_dict.get("keyCodes", [])
                for k in key_codes:
                    pyautogui.press(k.lower())
                return True

            elif action_type == "HOTKEY":
                key_codes = action_dict.get("keyCodes", [])
                if key_codes:
                    pyautogui.hotkey(*[k.lower() for k in key_codes])
                return True

            elif action_type == "TYPE_TEXT":
                text = action_dict.get("textPayload", "")
                if text:
                    pyautogui.write(text, interval=0.01)
                return True

        return False

input_controller = InputController()
