"""
Hendrix Desktop Companion - Platform Factory
Fábrica desacoplada que resuelve dinámicamente los controladores adecuados según el SO activo.
"""
import sys
from typing import Optional
from core.platform.interfaces import (
    IWindowManager,
    IInputController,
    ISystemOps,
    ICredentialVault,
    INetworkDiagnostic,
)

class PlatformFactory:
    def __init__(self):
        self._is_linux = sys.platform.startswith("linux")
        self._is_windows = sys.platform == "win32"
        self._is_darwin = sys.platform == "darwin"

        self._window_manager: Optional[IWindowManager] = None
        self._input_controller: Optional[IInputController] = None
        self._system_ops: Optional[ISystemOps] = None
        self._credential_vault: Optional[ICredentialVault] = None
        self._network_diagnostic: Optional[INetworkDiagnostic] = None

    @property
    def is_linux(self) -> bool:
        return self._is_linux

    @property
    def is_windows(self) -> bool:
        return self._is_windows

    @property
    def window_manager(self) -> IWindowManager:
        if self._window_manager is None:
            if self._is_linux:
                from core.platform.linux.linux_window_manager import LinuxWindowManager
                self._window_manager = LinuxWindowManager()
            elif self._is_windows:
                from automation.window_manager import WindowsWindowManager
                self._window_manager = WindowsWindowManager()
            else:
                from core.platform.linux.linux_window_manager import LinuxWindowManager
                self._window_manager = LinuxWindowManager()
        return self._window_manager

    def set_window_manager(self, wm: IWindowManager):
        """Permite inyección de mocks para pruebas unitarias."""
        self._window_manager = wm

    @property
    def input_controller(self) -> IInputController:
        if self._input_controller is None:
            if self._is_linux:
                from core.platform.linux.linux_input_controller import LinuxInputController
                self._input_controller = LinuxInputController()
            elif self._is_windows:
                from input.input_controller import WindowsInputController
                self._input_controller = WindowsInputController()
            else:
                from core.platform.linux.linux_input_controller import LinuxInputController
                self._input_controller = LinuxInputController()
        return self._input_controller

    def set_input_controller(self, ic: IInputController):
        self._input_controller = ic

    @property
    def system_ops(self) -> ISystemOps:
        if self._system_ops is None:
            if self._is_linux:
                from core.platform.linux.linux_system_ops import LinuxSystemOps
                self._system_ops = LinuxSystemOps()
            elif self._is_windows:
                from core.system_ops import WindowsSystemOps
                self._system_ops = WindowsSystemOps()
            else:
                from core.platform.linux.linux_system_ops import LinuxSystemOps
                self._system_ops = LinuxSystemOps()
        return self._system_ops

    def set_system_ops(self, ops: ISystemOps):
        self._system_ops = ops

    @property
    def credential_vault(self) -> ICredentialVault:
        if self._credential_vault is None:
            if self._is_linux:
                from core.platform.linux.linux_credential_vault import LinuxCredentialVault
                self._credential_vault = LinuxCredentialVault()
            elif self._is_windows:
                from automation.windows_credential_vault import WindowsCredentialVault
                self._credential_vault = WindowsCredentialVault()
            else:
                from core.platform.linux.linux_credential_vault import LinuxCredentialVault
                self._credential_vault = LinuxCredentialVault()
        return self._credential_vault

    def set_credential_vault(self, vault: ICredentialVault):
        self._credential_vault = vault

    @property
    def network_diagnostic(self) -> INetworkDiagnostic:
        if self._network_diagnostic is None:
            if self._is_linux:
                from core.platform.linux.linux_network_diagnostic import LinuxNetworkDiagnostic
                self._network_diagnostic = LinuxNetworkDiagnostic()
            else:
                from core.network_diagnostic import WindowsNetworkDiagnostic
                self._network_diagnostic = WindowsNetworkDiagnostic()
        return self._network_diagnostic

    def set_network_diagnostic(self, diag: INetworkDiagnostic):
        self._network_diagnostic = diag


# Singleton exportable
platform_factory = PlatformFactory()
