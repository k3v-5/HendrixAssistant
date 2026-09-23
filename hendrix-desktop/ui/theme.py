"""
Morado Neón OLED Void & Neon Purple Monolith Theme
Centralized visual tokens, ttk styles, and component builders for Hendrix Desktop.
"""

import tkinter as tk
from tkinter import ttk

# ==========================================
# PALETA DE COLORES (OLED VOID & NEON PURPLE)
# ==========================================
COLOR_VOID_BLACK = "#000000"
COLOR_VOID_SURFACE = "#090611"
COLOR_VOID_SURFACE_ELEVATED = "#130D22"
COLOR_VOID_BORDER = "#26163D"
COLOR_VOID_HOVER = "#1E1333"

COLOR_NEON_VIOLET = "#7C3AED"
COLOR_NEON_PURPLE = "#A855F7"
COLOR_NEON_LILAC = "#C084FC"

COLOR_NEON_CYAN = "#22D3EE"
COLOR_NEON_GREEN = "#10B981"
COLOR_NEON_AMBER = "#F59E0B"
COLOR_NEON_ROSE = "#F43F5E"

COLOR_TEXT_PRIMARY = "#F8FAFC"
COLOR_TEXT_SECONDARY = "#A1A1AA"
COLOR_TEXT_MUTED = "#64748B"

# ==========================================
# TIPOGRAFÍA Y FUENTES
# ==========================================
FONT_FAMILY = "Segoe UI"
FONT_CODE = "Consolas"

FONT_HEADER = (FONT_FAMILY, 15, "bold")
FONT_TITLE = (FONT_FAMILY, 11, "bold")
FONT_SUBTITLE = (FONT_FAMILY, 9)
FONT_BODY = (FONT_FAMILY, 9)
FONT_BODY_BOLD = (FONT_FAMILY, 9, "bold")
FONT_BODY_ITALIC = (FONT_FAMILY, 9, "italic")
FONT_CAPTION = (FONT_FAMILY, 8)
FONT_CAPTION_BOLD = (FONT_FAMILY, 8, "bold")
FONT_PIN_BADGE = (FONT_FAMILY, 13, "bold")
FONT_TERMINAL = (FONT_CODE, 9)


def setup_theme(root: tk.Tk) -> ttk.Style:
    """Configura los estilos globales de ttk adaptados a la estética Morado Neón OLED."""
    style = ttk.Style(root)
    style.theme_use("clam")

    # Base general
    style.configure(
        ".",
        background=COLOR_VOID_BLACK,
        foreground=COLOR_TEXT_PRIMARY,
        font=FONT_BODY
    )

    # Tarjetas y marcos elevados
    style.configure(
        "Card.TFrame",
        background=COLOR_VOID_SURFACE_ELEVATED,
        relief="flat"
    )

    style.configure(
        "Void.TFrame",
        background=COLOR_VOID_BLACK,
        relief="flat"
    )

    # Barras de Progreso Neon Purple
    style.configure(
        "TProgressbar",
        troughcolor=COLOR_VOID_SURFACE,
        background=COLOR_NEON_PURPLE,
        lightcolor=COLOR_NEON_LILAC,
        darkcolor=COLOR_NEON_VIOLET,
        bordercolor=COLOR_VOID_BORDER
    )

    # Checkbuttons estilo oscuro
    style.configure(
        "TCheckbutton",
        background=COLOR_VOID_SURFACE_ELEVATED,
        foreground=COLOR_TEXT_PRIMARY,
        font=FONT_BODY
    )
    style.map(
        "TCheckbutton",
        background=[("active", COLOR_VOID_SURFACE_ELEVATED)],
        foreground=[("active", COLOR_NEON_LILAC)]
    )

    # Notebook & Pestañas
    style.configure(
        "TNotebook",
        background=COLOR_VOID_BLACK,
        borderwidth=0
    )
    style.configure(
        "TNotebook.Tab",
        background=COLOR_VOID_SURFACE,
        foreground=COLOR_TEXT_SECONDARY,
        padding=[14, 8],
        font=FONT_BODY_BOLD,
        borderwidth=0
    )
    style.map(
        "TNotebook.Tab",
        background=[("selected", COLOR_NEON_VIOLET), ("active", COLOR_VOID_SURFACE_ELEVATED)],
        foreground=[("selected", COLOR_TEXT_PRIMARY), ("active", COLOR_NEON_LILAC)]
    )

    # Scrollbars
    style.configure(
        "Vertical.TScrollbar",
        background=COLOR_VOID_SURFACE_ELEVATED,
        troughcolor=COLOR_VOID_BLACK,
        arrowcolor=COLOR_NEON_LILAC,
        bordercolor=COLOR_VOID_BORDER,
        relief="flat"
    )
    style.map(
        "Vertical.TScrollbar",
        background=[("active", COLOR_NEON_VIOLET)]
    )

    # Combobox
    style.configure(
        "TCombobox",
        fieldbackground=COLOR_VOID_SURFACE,
        background=COLOR_VOID_SURFACE_ELEVATED,
        foreground=COLOR_TEXT_PRIMARY,
        arrowcolor=COLOR_NEON_LILAC,
        bordercolor=COLOR_VOID_BORDER,
        darkcolor=COLOR_VOID_BORDER,
        lightcolor=COLOR_VOID_BORDER
    )
    style.map(
        "TCombobox",
        fieldbackground=[("readonly", COLOR_VOID_SURFACE)],
        selectbackground=[("readonly", COLOR_NEON_VIOLET)],
        selectforeground=[("readonly", COLOR_TEXT_PRIMARY)]
    )

    return style


# ==========================================
# CONSTRUCTORES DE BOTONES Y WIDGETS ATÓMICOS
# ==========================================

def create_neon_button(parent, text: str, command=None, **kwargs) -> tk.Button:
    """Botón de acción primaria estilo Neón Violeta."""
    padx = kwargs.pop("padx", 12)
    pady = kwargs.pop("pady", 5)
    font = kwargs.pop("font", FONT_BODY_BOLD)
    return tk.Button(
        parent,
        text=text,
        command=command,
        font=font,
        bg=COLOR_NEON_VIOLET,
        fg=COLOR_TEXT_PRIMARY,
        activebackground=COLOR_NEON_PURPLE,
        activeforeground=COLOR_TEXT_PRIMARY,
        relief="flat",
        cursor="hand2",
        padx=padx,
        pady=pady,
        **kwargs
    )


def create_secondary_button(parent, text: str, command=None, **kwargs) -> tk.Button:
    """Botón secundario sobrio sobre superficie elevada."""
    padx = kwargs.pop("padx", 10)
    pady = kwargs.pop("pady", 4)
    font = kwargs.pop("font", FONT_CAPTION_BOLD)
    return tk.Button(
        parent,
        text=text,
        command=command,
        font=font,
        bg=COLOR_VOID_BORDER,
        fg=COLOR_TEXT_SECONDARY,
        activebackground=COLOR_VOID_HOVER,
        activeforeground=COLOR_NEON_LILAC,
        relief="flat",
        cursor="hand2",
        padx=padx,
        pady=pady,
        **kwargs
    )


def create_accent_button(parent, text: str, command=None, **kwargs) -> tk.Button:
    """Botón de acción destacada (Neon Purple)."""
    padx = kwargs.pop("padx", 12)
    pady = kwargs.pop("pady", 5)
    font = kwargs.pop("font", FONT_BODY_BOLD)
    return tk.Button(
        parent,
        text=text,
        command=command,
        font=font,
        bg=COLOR_NEON_PURPLE,
        fg=COLOR_TEXT_PRIMARY,
        activebackground=COLOR_NEON_LILAC,
        activeforeground=COLOR_VOID_BLACK,
        relief="flat",
        cursor="hand2",
        padx=padx,
        pady=pady,
        **kwargs
    )


def create_danger_button(parent, text: str, command=None, **kwargs) -> tk.Button:
    """Botón de acción destructiva o desconexión."""
    padx = kwargs.pop("padx", 8)
    pady = kwargs.pop("pady", 3)
    font = kwargs.pop("font", FONT_CAPTION_BOLD)
    return tk.Button(
        parent,
        text=text,
        command=command,
        font=font,
        bg=COLOR_NEON_ROSE,
        fg=COLOR_TEXT_PRIMARY,
        activebackground="#BE123C",
        activeforeground=COLOR_TEXT_PRIMARY,
        relief="flat",
        cursor="hand2",
        padx=padx,
        pady=pady,
        **kwargs
    )


def create_warning_button(parent, text: str, command=None, **kwargs) -> tk.Button:
    """Botón de alerta o desconexión temporal."""
    padx = kwargs.pop("padx", 8)
    pady = kwargs.pop("pady", 3)
    font = kwargs.pop("font", FONT_CAPTION_BOLD)
    return tk.Button(
        parent,
        text=text,
        command=command,
        font=font,
        bg=COLOR_NEON_AMBER,
        fg=COLOR_VOID_BLACK,
        activebackground="#D97706",
        activeforeground=COLOR_VOID_BLACK,
        relief="flat",
        cursor="hand2",
        padx=padx,
        pady=pady,
        **kwargs
    )
