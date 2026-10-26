# Reel Counter

Facebook, TikTok, Instagram aur YouTube mein dekhi gayi reels ginta hai. Screen ke top par chota counter dikhata hai aur app ke andar statistics hain (aaj, kal, 7 din, 30 din, sab se zyada din/waqt, app ke hisaab se).

## APK banane ke steps (GitHub par, free)

1. github.com par free account banayen (agar nahi hai).
2. Naya repository banayen (naam: `ReelCounter`).
3. Is zip ko PC par extract karein. Repository page par **"uploading an existing file"** dabayen aur extract kiye hue folder ke **andar ki saari files aur folders** (khaas taur par chhupa hua `.github` folder bhi) drag-drop karein. Phir **Commit changes**.
4. Repository mein **Actions** tab kholein. "Build APK" apne aap chalega (karib 5 minute). Agar Actions band dikhe to "Enable" dabayen.
5. Build khatam hone par repository ke home page par right side **Releases** mein latest release kholein aur `ReelCounter.apk` apne phone par download karein.
6. Phone mein APK kholein, "Install unknown apps" ki ijazat dein, install karein.

## App chalane ke steps

1. App kholein, **Accessibility on karein** dabayen, **Reel Counter** ko ON karein.
2. Android 13 ya naya ho aur "Restricted setting" aaye: Settings > Apps > Reel Counter > upar right teen nuqte > **Allow restricted settings**, phir dobara ON karein.
3. Koi reel app kholein. Top par `Aaj: N` ka counter nazar aaye ga.

## Zaroori baatein

- Ginti andaazan hai: har reel swipe ko ek reel maana jata hai (debounce 1 second).
- Har app ka layout alag hota hai aur update ke saath badalta hai. Agar kisi app mein ginti na ho ya zyada ho, `ReelApp.kt` mein us app ke `reelIds` lafz theek karne parte hain.
- App ke paas INTERNET permission nahi hai, data sirf phone mein rehta hai.
