# -*- coding: utf-8 -*-
"""Gera screenshots estilizadas para a Google Play a partir dos prints brutos."""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

SRC_DIR = r"C:\Users\TECNOMOTOR\Downloads\IMAGENS GOOGLE PLAY"
OUT_DIR = os.path.join(SRC_DIR, "prontas_play_store")
os.makedirs(OUT_DIR, exist_ok=True)

FONT_BLACK = r"C:\Windows\Fonts\Rubik-Bold.ttf"
FONT_BOLD = r"C:\Windows\Fonts\Rubik-Bold.ttf"
FONT_REG = r"C:\Windows\Fonts\segoeui.ttf"

W, H = 1080, 1920

SHOTS = [
    dict(
        file="Captura de tela 2026-08-08 100414.png",
        title="Tudo sobre seu carro\nem um só lugar",
        subtitle="Km rodados, documentos e avisos sempre à mão",
        accent=(59, 130, 246),
    ),
    dict(
        file="Captura de tela 2026-08-08 100447.png",
        title="Nunca mais esqueça\numa manutenção",
        subtitle="Óleo, freio, pneu, revisão... escolha e pronto",
        accent=(99, 102, 241),
    ),
    dict(
        file="Captura de tela 2026-08-08 100533.png",
        title="Cadastre lembretes\nem segundos",
        subtitle="Escaneie a nota fiscal e preencha tudo automaticamente",
        accent=(37, 99, 235),
    ),
    dict(
        file="Captura de tela 2026-08-08 100640.png",
        title="Acompanhe a saúde\ndo seu veículo",
        subtitle="Score de manutenção e gastos mês a mês",
        accent=(234, 179, 8),
    ),
    dict(
        file="Captura de tela 2026-08-08 100729.png",
        title="Fique de olho\nnos vencidos",
        subtitle="Veja valores, datas e prestadores de cada serviço",
        accent=(239, 68, 68),
    ),
    dict(
        file="Captura de tela 2026-08-08 100813.png",
        title="Tudo o que você\nprecisa, a um toque",
        subtitle="Biblioteca, novidades, segurança e configurações",
        accent=(168, 85, 247),
    ),
    dict(
        file="Captura de tela 2026-08-08 100836.png",
        title="Cadastre quantos\nveículos quiser",
        subtitle="Carro, moto ou frota: cada um com seu histórico",
        accent=(34, 197, 94),
    ),
]


def make_gradient_bg(w, h):
    top = (13, 16, 26)
    bottom = (24, 28, 46)
    img = Image.new("RGB", (w, h), top)
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        for x in range(0, w, 4):
            for xx in range(x, min(x + 4, w)):
                px[xx, y] = (r, g, b)
    return img


def add_glow(img, accent, center, radius):
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse(
        [center[0] - radius, center[1] - radius, center[0] + radius, center[1] + radius],
        fill=accent + (75,),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(150))
    img.paste(Image.alpha_composite(img.convert("RGBA"), glow).convert("RGB"), (0, 0))
    return img


def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, size[0], size[1]], radius=radius, fill=255)
    return mask


def draw_phone_frame(base, screenshot_path, phone_box):
    x0, y0, x1, y1 = phone_box
    frame_w = x1 - x0
    frame_h = y1 - y0
    bezel = 18
    corner_r = 70

    frame = Image.new("RGBA", (frame_w, frame_h), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame)
    fd.rounded_rectangle([0, 0, frame_w, frame_h], radius=corner_r, fill=(20, 20, 24, 255))
    fd.rounded_rectangle(
        [bezel * 0.4, bezel * 0.4, frame_w - bezel * 0.4, frame_h - bezel * 0.4],
        radius=corner_r - 8,
        outline=(70, 72, 80, 255),
        width=3,
    )

    inner_box = (bezel, bezel, frame_w - bezel, frame_h - bezel)
    iw = inner_box[2] - inner_box[0]
    ih = inner_box[3] - inner_box[1]

    shot = Image.open(screenshot_path).convert("RGB")
    sw, sh = shot.size
    scale = max(iw / sw, ih / sh)
    shot = shot.resize((int(sw * scale), int(sh * scale)), Image.LANCZOS)
    left = (shot.width - iw) // 2
    top = (shot.height - ih) // 2
    shot = shot.crop((left, top, left + iw, top + ih))

    inner_r = corner_r - 14
    mask = rounded_mask((iw, ih), inner_r)
    frame.paste(shot, (inner_box[0], inner_box[1]), mask)

    notch_w, notch_h = 150, 28
    nd_box = [
        (frame_w - notch_w) // 2,
        bezel + 14,
        (frame_w + notch_w) // 2,
        bezel + 14 + notch_h,
    ]
    fd.rounded_rectangle(nd_box, radius=notch_h // 2, fill=(10, 10, 12, 255))

    shadow = Image.new("RGBA", base.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle(
        [x0 + 12, y0 + 24, x1 + 12, y1 + 24], radius=corner_r, fill=(0, 0, 0, 120)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(30))
    base_rgba = Image.alpha_composite(base.convert("RGBA"), shadow)
    base_rgba.paste(frame, (x0, y0), frame)
    return base_rgba.convert("RGB")


def fit_font(draw, text, font_path, max_width, start_size, min_size=40):
    size = start_size
    while size > min_size:
        font = ImageFont.truetype(font_path, size)
        widths = [draw.textbbox((0, 0), line, font=font)[2] for line in text.split("\n")]
        if max(widths) <= max_width:
            return font
        size -= 4
    return ImageFont.truetype(font_path, min_size)


def draw_centered_multiline(draw, text, font, center_x, top_y, fill, line_spacing=10):
    lines = text.split("\n")
    y = top_y
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=font)
        w = bbox[2] - bbox[0]
        h = bbox[3] - bbox[1]
        draw.text((center_x - w / 2, y - bbox[1]), line, font=font, fill=fill)
        y += h + line_spacing
    return y


def build_image(shot_cfg, index):
    accent = shot_cfg["accent"]
    img = make_gradient_bg(W, H)
    img = add_glow(img, accent, (W // 2, int(H * 0.02)), 700)

    draw = ImageDraw.Draw(img)

    title_font = fit_font(draw, shot_cfg["title"], FONT_BLACK, W - 110, 92, 56)
    y = draw_centered_multiline(draw, shot_cfg["title"], title_font, W // 2, 118, (255, 255, 255), line_spacing=6)

    subtitle_font = fit_font(draw, shot_cfg["subtitle"], FONT_REG, W - 160, 42, 30)
    draw_centered_multiline(draw, shot_cfg["subtitle"], subtitle_font, W // 2, y + 22, (200, 205, 215))

    phone_w = 760
    phone_h = int(phone_w * 1920 / 900)
    px0 = (W - phone_w) // 2
    py0 = 470
    img = draw_phone_frame(img, os.path.join(SRC_DIR, shot_cfg["file"]), (px0, py0, px0 + phone_w, py0 + phone_h))

    out_path = os.path.join(OUT_DIR, f"play_store_{index+1:02d}.png")
    img.save(out_path, quality=95)
    print("Salvo:", out_path)


if __name__ == "__main__":
    for i, cfg in enumerate(SHOTS):
        build_image(cfg, i)
