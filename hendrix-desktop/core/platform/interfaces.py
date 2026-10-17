"""
Hendrix Desktop Companion - Interfaces de Plataforma
Contratos puros para desacoplar operaciones dependientes del Sistema Operativo.
"""
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional, Tuple

class IWindowManager(ABC):
    """Contrato para inspección y control de ventanas del sistema."""

    @abstractmethod
    def get_taskbar_windows(self) -> List[Dict[str, Any]]:
        """Retorna la lista de ventanas activas visibles en la barra de tareas o dock."""
        pass

    @abstractmethod
    def focus_window(self, hwnd_or_id: Any) -> bool:
        """Trae la ventana al frente y le asigna el foco de entrada."""
        pass

    @abstractmethod
    def restore_window(self, hwnd_or_id: Any) -> bool:
        """Restaura una ventana minimizada."""
        pass

    @abstractmethod
    def close_window(self, hwnd_or_id: Any) -> bool:
        """Solicita el cierre seguro de la ventana."""
        pass

    @abstractmethod
    def get_foreground_window(self) -> Any:
        """Retorna el identificador o handle de la ventana activa en primer plano."""
        pass

    @abstractmethod
    def get_window_title(self, hwnd_or_id: Any) -> str:
        """Retorna el título legible de una ventana."""
        pass

    @abstractmethod
    def get_window_rect(self, hwnd_or_id: Any) -> Dict[str, int]:
        """Retorna las coordenadas {left, top, right, bottom} de la ventana."""
        pass


class IInputController(ABC):
    """Contrato para inyección de eventos de ratón y teclado."""

    @property
    @abstractmethod
    def screen_size(self) -> Tuple[int, int]:
        """Retorna (width, height) de la pantalla principal o virtual."""
        pass

    @abstractmethod
    def move_cursor(self, x: int, y: int) -> None:
        """Mueve el cursor a las coordenadas absolutas (x, y)."""
        pass

    @abstractmethod
    def click(self, x: int, y: int, button: str = "left", double: bool = False) -> None:
        """Ejecuta un clic o doble clic."""
        pass

    @abstractmethod
    def mouse_down(self, x: int, y: int, button: str = "left") -> None:
        """Presiona un botón del ratón."""
        pass

    @abstractmethod
    def mouse_up(self, x: int, y: int, button: str = "left") -> None:
        """Libera un botón del ratón."""
        pass

    @abstractmethod
    def scroll(self, x: int, y: int, amount: int) -> None:
        """Realiza scroll vertical (positivo arriba, negativo abajo)."""
        pass

    @abstractmethod
    def press_key(self, key: str) -> None:
        """Presiona una tecla especial o atajo."""
        pass

    @abstractmethod
    def type_text(self, text: str) -> None:
        """Escribe una cadena de texto."""
        pass


class ISystemOps(ABC):
    """Contrato para operaciones del sistema operativo y energía."""

    @abstractmethod
    def shutdown(self) -> bool:
        """Apaga el equipo de forma segura."""
        pass

    @abstractmethod
    def reboot(self) -> bool:
        """Reinicia el equipo."""
        pass

    @abstractmethod
    def suspend(self) -> bool:
        """Pone el equipo en estado de suspensión / reposo."""
        pass

    @abstractmethod
    def lock(self) -> bool:
        """Bloquea la sesión de usuario activa."""
        pass

    @abstractmethod
    def set_volume(self, level: int) -> bool:
        """Ajusta el volumen maestro (0 a 100)."""
        pass

    @abstractmethod
    def get_volume(self) -> int:
        """Retorna el nivel de volumen actual (0 a 100)."""
        pass

    @abstractmethod
    def mute(self, state: bool) -> bool:
        """Silencia o des-silencia el audio del sistema."""
        pass


class ICredentialVault(ABC):
    """Contrato para almacenamiento seguro de credenciales y tokens."""

    @abstractmethod
    def store_credential(self, target_name: str, username: str, secret: str) -> bool:
        """Guarda o actualiza una credencial de forma cifrada."""
        pass

    @abstractmethod
    def read_credential(self, target_name: str) -> Optional[Tuple[str, str]]:
        """Recupera (username, secret) para el objetivo especificado, o None si no existe."""
        pass

    @abstractmethod
    def delete_credential(self, target_name: str) -> bool:
        """Elimina una credencial almacenada."""
        pass


class INetworkDiagnostic(ABC):
    """Contrato para diagnóstico de red y cortafuegos."""

    @abstractmethod
    def check_firewall_rule(self, rule_name: str = "HendrixCompanion", port: int = 8765) -> Dict[str, Any]:
        """Verifica si los puertos del servicio están abiertos en el cortafuegos."""
        pass

    @abstractmethod
    def repair_firewall_rule(self, port: int = 8765) -> Tuple[bool, str]:
        """Intenta abrir o reparar la regla de firewall para el puerto."""
        pass
