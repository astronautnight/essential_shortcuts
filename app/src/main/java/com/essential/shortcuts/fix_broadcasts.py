import os
import re

directory = "c:/Users/rajas/Downloads/AndroidStudioProjects/ClaudeEssential/app/src/main/java/com/essential/shortcuts/"

files = [
    "ImageCompressorActivity.kt",
    "ImageToWebpActivity.kt",
    "TextToPdfActivity.kt",
    "RoundedImageActivity.kt",
    "PixelFramesActivity.kt",
    "PdfCombineActivity.kt",
    "ImageToPdfActivity.kt",
    "JpgPngConverterActivity.kt",
    "ImageResizerActivity.kt",
    "ImageConverterActivity.kt"
]

for filename in files:
    filepath = os.path.join(directory, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    original_content = content

    # 1. Add setPackage(packageName) to ACTION_STEP_2
    content = content.replace(
        'sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2"))',
        'sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))'
    )

    # 2. Remove ACTION_STEP_3 completely (it appears with setResult and finish)
    content = re.sub(
        r'sendBroadcast\(\s*Intent\(\s*"com\.essential\.shortcuts\.ACTION_STEP_3"\s*\)\s*\)\s*setResult\(RESULT_OK\)\s*finish\(\)',
        r'setResult(RESULT_OK)\n            finish()',
        content
    )
    
    # Also in case they just have the broadcast without finish (from my previous edits)
    content = re.sub(
        r'// Step 3:.*?\n\s*sendBroadcast\(Intent\("com\.essential\.shortcuts\.ACTION_STEP_3"\)\)',
        r'',
        content
    )

    # 3. For the 3 files where I removed finish(), add it back.
    # We look for setResult(RESULT_OK) that is NOT followed by finish()
    # Let's just find `setResult(RESULT_OK)` that is at the end of save logic
    # Actually, we can just replace `setResult(RESULT_OK)` with `setResult(RESULT_OK)\n            finish()` if `finish()` isn't on the next line.
    content = re.sub(
        r'setResult\(RESULT_OK\)(?!\s*finish\(\))',
        r'setResult(RESULT_OK)\n            finish()',
        content
    )

    if content != original_content:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"Updated {filename}")
    else:
        print(f"No changes in {filename}")
