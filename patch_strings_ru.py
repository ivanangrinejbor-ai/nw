import re

path_ru = r'catroid\src\main\res\values-ru\strings.xml'
with open(path_ru, 'r', encoding='utf-8') as f:
    content = f.read()

# Insert brick_set_ragdoll after brick_point_touch_direction
if 'brick_set_ragdoll' not in content:
    marker = 'name="brick_point_touch_direction"'
    idx = content.find(marker)
    if idx == -1:
        print('ERROR: brick_point_touch_direction not found in RU')
    else:
        end = content.index('</string>', idx) + len('</string>')
        new_line = '\n    <string formatted="false" name="brick_set_ragdoll">\u0417\u0430\u0440\u0435\u0433\u0434\u043e\u043b\u0438\u0442\u044c (1=\u0432\u043a\u043b, 0=\u0432\u044b\u043a\u043b):</string>'
        content = content[:end] + new_line + content[end:]
        print('OK: brick_set_ragdoll inserted in RU')
else:
    print('SKIP: brick_set_ragdoll already in RU')

# Insert formula_sprite_ragdolled after formula_sprite_visible
if 'formula_sprite_ragdolled' not in content:
    marker2 = 'name="formula_sprite_visible"'
    idx2 = content.find(marker2)
    if idx2 == -1:
        print('ERROR: formula_sprite_visible not found in RU')
    else:
        end2 = content.index('</string>', idx2) + len('</string>')
        new_line2 = '\n    <string name="formula_sprite_ragdolled" formatted="false">\u0421\u043f\u0440\u0430\u0439\u0442_\u0437\u0430\u0440\u0435\u0433\u0434\u043e\u043b\u043b\u0435\u043d</string>'
        content = content[:end2] + new_line2 + content[end2:]
        print('OK: formula_sprite_ragdolled inserted in RU')
else:
    print('SKIP: formula_sprite_ragdolled already in RU')

with open(path_ru, 'w', encoding='utf-8') as f:
    f.write(content)
print('DONE RU')
