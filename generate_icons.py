import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_app_icon(size, is_round=False):
    # Base canvas
    scale = 4
    canvas_size = size * scale
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Rounded rect or circle background
    margin = int(canvas_size * 0.05)
    rect = [margin, margin, canvas_size - margin, canvas_size - margin]
    
    # Elegant Deep Indigo Gradient / Royal Purple (#4A148C to #673AB7)
    # Generate vertical gradient
    bg_img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg_img)
    
    top_color = (103, 58, 183) # Deep Violet
    bottom_color = (49, 27, 146) # Deep Royal Indigo
    
    for y in range(canvas_size):
        ratio = y / canvas_size
        r = int(top_color[0] * (1 - ratio) + bottom_color[0] * ratio)
        g = int(top_color[1] * (1 - ratio) + bottom_color[1] * ratio)
        b = int(top_color[2] * (1 - ratio) + bottom_color[2] * ratio)
        bg_draw.line([(0, y), (canvas_size, y)], fill=(r, g, b, 255))
        
    # Mask for shape
    mask = Image.new("L", (canvas_size, canvas_size), 0)
    mask_draw = ImageDraw.Draw(mask)
    if is_round:
        mask_draw.ellipse(rect, fill=255)
    else:
        radius = int(canvas_size * 0.22)
        mask_draw.rounded_rectangle(rect, radius=radius, fill=255)
        
    img.paste(bg_img, (0, 0), mask)
    
    # Draw Inner Keyboard keycap frame / pedestal
    draw = ImageDraw.Draw(img)
    key_margin_x = int(canvas_size * 0.16)
    key_margin_top = int(canvas_size * 0.14)
    key_margin_bottom = int(canvas_size * 0.18)
    key_rect = [key_margin_x, key_margin_top, canvas_size - key_margin_x, canvas_size - key_margin_bottom]
    key_radius = int(canvas_size * 0.16)
    
    # Keycap subtle drop shadow
    shadow_offset = int(canvas_size * 0.03)
    shadow_rect = [key_rect[0], key_rect[1] + shadow_offset, key_rect[2], key_rect[3] + shadow_offset]
    draw.rounded_rectangle(shadow_rect, radius=key_radius, fill=(25, 10, 80, 140))
    
    # Keycap surface
    draw.rounded_rectangle(key_rect, radius=key_radius, fill=(255, 255, 255, 235))
    
    # Inner border / bevel on keycap
    draw.rounded_rectangle(key_rect, radius=key_radius, outline=(255, 255, 255, 180), width=int(canvas_size * 0.012))
    
    # Subtle Thai flag decorative accent stripe below keycap
    flag_w = int(canvas_size * 0.44)
    flag_h = int(canvas_size * 0.032)
    flag_x = (canvas_size - flag_w) // 2
    flag_y = int(canvas_size * 0.86)
    # Red - White - Blue - White - Red
    stripe_w = flag_w
    d_y = flag_y
    draw.rounded_rectangle([flag_x, d_y, flag_x + stripe_w, d_y + flag_h], radius=int(flag_h / 2), fill=(255, 215, 0, 220)) # Golden accent bar
    
    # Load Thai Font for "ก"
    font_path = "C:/Windows/Fonts/LeelaUIb.ttf"
    if not os.path.exists(font_path):
        font_path = "C:/Windows/Fonts/tahomabd.ttf"
        
    font_size = int(canvas_size * 0.44)
    font = ImageFont.truetype(font_path, font_size)
    
    # Draw Thai character "ก" (The King of Thai letters / Phase 1 Anchor)
    thai_char = "ก"
    bbox = draw.textbbox((0, 0), thai_char, font=font)
    char_w = bbox[2] - bbox[0]
    char_h = bbox[3] - bbox[1]
    
    # Position in center of keycap
    key_center_x = (key_rect[0] + key_rect[2]) / 2
    key_center_y = (key_rect[1] + key_rect[3]) / 2 - int(canvas_size * 0.03)
    
    pos_x = key_center_x - char_w / 2 - bbox[0]
    pos_y = key_center_y - char_h / 2 - bbox[1]
    
    # Elegant deep indigo text with subtle gold accent
    draw.text((pos_x, pos_y), thai_char, font=font, fill=(74, 20, 140, 255))
    
    # Small Kedmanee sub-label "D" on top-left of key
    sub_font_size = int(canvas_size * 0.10)
    sub_font = ImageFont.truetype("C:/Windows/Fonts/tahomabd.ttf", sub_font_size)
    draw.text((key_rect[0] + int(canvas_size * 0.05), key_rect[1] + int(canvas_size * 0.04)), "TH", font=sub_font, fill=(156, 39, 176, 200))
    
    # Downscale for ultra-sharp anti-aliased output
    final_img = img.resize((size, size), Image.Resampling.LANCZOS)
    return final_img

# Target directory
res_dir = r"C:\Users\Jordan\AndroidStudioProjects\LearnThai\app\src\main\res"

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

for folder, size in densities.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    # Standard icon
    sq_img = create_app_icon(size, is_round=False)
    sq_img.save(os.path.join(folder_path, "ic_launcher.webp"), "WEBP", quality=95)
    
    # Round icon
    round_img = create_app_icon(size, is_round=True)
    round_img.save(os.path.join(folder_path, "ic_launcher_round.webp"), "WEBP", quality=95)

# Also create high-res web/store favicon (512x512 PNG and 32x32 favicon.ico)
fav_512 = create_app_icon(512, is_round=False)
fav_512.save(r"C:\Users\Jordan\AndroidStudioProjects\LearnThai\app\src\main\res\drawable\app_icon_512.png", "PNG")

fav_32 = create_app_icon(32, is_round=False)
fav_32.save(r"C:\Users\Jordan\AndroidStudioProjects\LearnThai\app\src\main\res\drawable\favicon.ico", format="ICO")

print("All app icons, launcher mipmaps, and favicon successfully generated!")
