import os
from PIL import Image

img_path = r"C:\Users\rajas\.gemini\antigravity-ide\brain\3918551a-8b6b-4d27-b852-c988981e4654\media__1783763728803.png"
if os.path.exists(img_path):
    img = Image.open(img_path)
    w, h = img.size
    cx = w // 2
    
    # Scan left and right at y = 30 to find the notch boundaries
    notch_left = cx
    while notch_left > 0:
        p = img.getpixel((notch_left, 30))
        if p[0] > 100 or p[1] > 100 or p[2] > 100: # Not black
            break
        notch_left -= 1
        
    notch_right = cx
    while notch_right < w:
        p = img.getpixel((notch_right, 30))
        if p[0] > 100 or p[1] > 100 or p[2] > 100: # Not black
            break
        notch_right += 1
        
    print(f"Notch Left: {notch_left + 1}")
    print(f"Notch Right: {notch_right - 1}")
    print(f"Notch Width: { (notch_right - 1) - (notch_left + 1) + 1 }")
    print(f"Percentages: Left={ (notch_left + 1)/w*100:.2f}%, Right={ (notch_right - 1)/w*100:.2f}%")
