import os

with open('app/src/main/res/layout/fragment_settings.xml', 'r', encoding='utf-8') as f:
    lines = f.readlines()

start_idx = -1
end_idx = -1
for i, line in enumerate(lines):
    if '<com.essential.shortcuts.BouncyNestedScrollView' in line:
        start_idx = i
    if '</com.essential.shortcuts.BouncyNestedScrollView>' in line:
        end_idx = i

child_start = -1
for i in range(start_idx, end_idx):
    if '<LinearLayout' in lines[i]:
        child_start = i
        break

content = ['<?xml version="1.0" encoding="utf-8"?>\n'] + lines[child_start:end_idx]

with open('app/src/main/res/layout/layout_settings_content.xml', 'w', encoding='utf-8') as f:
    f.writelines(content)

new_settings_lines = lines[:start_idx] + [
    '    <androidx.recyclerview.widget.RecyclerView\n',
    '        android:id="@+id/scroll_view"\n',
    '        android:layout_width="match_parent"\n',
    '        android:layout_height="match_parent"\n',
    '        app:layout_behavior="@string/appbar_scrolling_view_behavior"\n',
    '        android:paddingBottom="100dp"\n',
    '        android:clipToPadding="false"\n',
    '        android:overScrollMode="always" />\n'
] + lines[end_idx+1:]

with open('app/src/main/res/layout/fragment_settings.xml', 'w', encoding='utf-8') as f:
    f.writelines(new_settings_lines)

print('Extracted and replaced successfully.')
