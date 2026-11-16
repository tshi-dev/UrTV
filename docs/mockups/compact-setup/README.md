# Compact setup screens (4.4)

The owner: «The setup page and the installation screens need to be smaller, like I said about the settings. It's good to have big stuff, but they're just toooo much.»
Each picture: **before** on the left, **after** on the right, a 1920×1080 TV at density 2, drawn in HTML with a stand-in font (DejaVu Sans, not Thmanyah):
the sizes are the app's, the drawing is not a screenshot of the TV.

The sizes (android/launcher/.../Compact.java, pinned by tests/compact_test.py), as in the compact settings (work/433-design, docs/mockups/settings-compact):
a window at most 560 dp wide in the safe area, a 24 sp title, 16–18 sp rows and buttons, 13–14 sp second lines (never under 13), buttons 44 dp with 18 sp
words, the setup's QR 88 dp (about 180 px) on the left. The focus ring stays.

| Picture | Screen |
|---|---|
| [setup-start.png](setup-start.png) | «إعداد تلفزيوني», the start |
| [setup-plan.png](setup-plan.png) | the plan and the suggested apps |
| [setup-steps.png](setup-steps.png) | the steps: one row of words above the work (no side rail at 560 dp) |
| [update.png](update.png) | «تحديث تلفزيوني» |
| [update-done.png](update-done.png) | «تم التحديث» |
| [install-saver.png](install-saver.png) | «ثبّت الجزء الثاني» |
| [extras-offer.png](extras-offer.png) | «إضافات تلفزيونك», the offer |
| [rescue.png](rescue.png) | «إنقاذ تلفزيونك» |
| [factory-confirm.png](factory-confirm.png) | «تخلّص من واجهة المصنع», «متأكد؟» |
