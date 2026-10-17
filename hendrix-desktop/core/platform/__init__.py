"""
Hendrix Desktop Companion - Abstracción de Plataforma
Interfaces y contratos desacoplados para soporte multiplataforma (Linux / Windows).
"""
from core.platform.interfaces import (
    IWindowManager,
    IInputController,
    ISystemOps,
    ICredentialVault,
    INetworkDiagnostic,
)
from core.platform.factory import platform_factory

__all__ = [
    "IWindowManager",
    "IInputController",
    "ISystemOps",
    "ICredentialVault",
    "INetworkDiagnostic",
    "platform_factory",
]
