# Seamless Portals (Minecraft 1.12.2, Forge 14.23.5.2860)

Uzluksiz (seamless) portal modi: portal ichidan narigi joy ko'rinadi, ichiga kirsangiz kamera uzilmaydi.

## Ishlatish
- Item: **Portal Wand** (Creative > Misc yoki `/give @p seamlessportals:portal_wand`)
- Blokning ustiga/yoniga o'ng tugma = 1-portal (siz qaragan tomonga qaraydi). Boshqa joyga o'ng tugma = 2-portal, juftlik tayyor.
- Portal: 2 keng x 3 baland, havoda turadi (eshik oralig'iga qo'ying). Ikki tomoni ham ishlaydi.
- Sneak + havoga o'ng tugma = yaqin (8 blok) portallarni o'chirish.

## Build (Termux'da emas, GitHub Actions'da)
1. Papkani GitHub repo'ga yuklang.
2. Actions > "Build mod" > Run workflow. Artifact: `seamlessportals-jar`.
3. `.jar` ni PojavLauncher'da Forge 1.12.2 `mods/` papkasiga tashlang.

## Sozlamalar (`config/seamlessportals.cfg`)
`maxViews`, `resolutionScale`, `portalRenderDistance`, `maxDistance` — telefon uchun pastroq qiymatlar tavsiya.

## Cheklovlar
- Faqat bir dunyo (dimension) ichida; faqat vertikal (devor) portallar.
- Birinchi shaxs ko'rinishida ishlaydi (3-shaxs ko'rinishida portal ichi chizilmaydi).
- Portal ichida o'z tanangiz ko'rinmaydi, moblar portaldan o'tmaydi.
- Pojav'da: Holy-GL4ES/Zink renderer, stencil buffer kerak.
# sp
# sp
