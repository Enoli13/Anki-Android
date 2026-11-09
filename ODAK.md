# AnkiDroid Odak

Bu depo [AnkiDroid](https://github.com/ankidroid/Anki-Android) projesinin kişisel bir fork'udur ve
`odak` dalında **değiştirilmiştir** (Kasım 2026'dan itibaren). Taban sürüm: AnkiDroid v2.25.3.
Lisans: GNU GPL v3 veya sonrası (`COPYING`). AnkiDroid ekibinin resmi bir sürümü değildir.

## Ne eklendi
Odak, Reels/Shorts/TikTok açma dürtüsünü birkaç Anki kartlık kısa bir tekrara çevirir.
Kart motoru yoktur: kartlar ve ilerleme tamamen AnkiDroid koleksiyonundadır.

| Parça | Yer |
|---|---|
| AnkiDroid ile bağlantı (başlatma, açılış ekranı, Reviewer'a yönlendirme) | `AnkiDroid/src/main/java/com/ichi2/anki/odak/OdakHooks.kt` |
| Odak ayarları, günlük, rapor (yerel JSON) | `.../odak/data/` |
| Erişilebilirlik servisi, kaydırma sayacı, toplu bildirim dinleyicisi | `.../odak/service/` |
| Hatırlatma ve toplu bildirim işleri | `.../odak/work/` |
| Ekranlar (Compose) | `.../odak/ui/` |

AnkiDroid'in kendi dosyalarındaki değişiklikler küçük tutuldu:
`AnkiDroidApp.kt` (başlatma), `Reviewer.kt` (durdurma ekranından gelince N kartta bitirme ve cevap günlüğü),
`DeckPicker.kt` + `NavigationDrawerActivity.kt` + menüler (Odak girişi), `AndroidManifest.xml`, `lint.gradle`.

## Derleme
`odak` dalına her gönderimde `.github/workflows/odak.yml` APK'yı derler ve Releases'e ekler.
Sürüm numarası: AnkiDroid sürümü + `ODAK_VERSION` dosyası (ör. `v2.25.3-odak.1`).
Elle: `./gradlew :AnkiDroid:assemblePlayRelease -PcustomSuffix=odak -PcustomName="AnkiDroid Odak" -Duniversal-apk=true`
