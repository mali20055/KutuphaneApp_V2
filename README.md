# kutuphaneApp (Kitaplık Yönetim Uygulaması)

Bu proje, kullanıcıların kendi kişisel kitaplıklarını yönetmelerine, yeni kitaplar keşfetmelerine ve kitap detaylarını takip etmelerine olanak tanıyan kapsamlı bir Android uygulamasıdır.

## 🚀 Özellikler

- **Kitap Arama:** Google Books ve Open Library API'lerini kullanarak geniş bir veri tabanında arama yapma.
- **Barkod Tarama:** ML Kit entegrasyonu ile kitapların ISBN barkodlarını tarayarak hızlıca kitap bilgilerine ulaşma.
- **Kişisel Kitaplık:** Kitapları "Okunacaklar", "Okunuyor" veya "Okundu" gibi kategorilerle takip etme.
- **İstatistikler:** Kitaplık verilerine dayalı okuma istatistiklerini görüntüleme.
- **Kullanıcı Yönetimi:** Firebase Auth ile güvenli kayıt ve giriş sistemleri.
- **Veri Senkronizasyonu:** Firebase Firestore ile verilerin bulut üzerinde saklanması.
- **Modern Arayüz:** Material Design bileşenleri ile geliştirilmiş, kullanıcı dostu arayüz.

## 🛠 Kullanılan Teknolojiler

- **Dil:** Kotlin
- **Mimari:** MVVM (Model-View-ViewModel)
- **Ağ İletişimi:** Retrofit & Gson (REST API entegrasyonu)
- **Görsel İşleme:** Glide (Resim yükleme ve önbelleğe alma)
- **Asenkron Programlama:** Kotlin Coroutines & Flow
- **Veri Tabanı & Auth:** Firebase (Auth, Firestore)
- **Yapay Zeka:** Google ML Kit (Barkod Tarama)
- **Navigasyon:** Android Jetpack Navigation Component
- **UI:** XML, ConstraintLayout, Material Design Components

## 📦 Kurulum

1. Bu depoyu klonlayın:
   ```bash
   git clone https://github.com/kullaniciadi/kutuphaneApp.git
   ```
2. Projeyi Android Studio ile açın.
3. `local.properties` dosyanıza veya `BuildConfig` ayarlarınıza Google Books API anahtarınızı ekleyin.
4. Kendi Firebase projenizi oluşturun ve `google-services.json` dosyasını `app/` dizinine ekleyin.
5. Projeyi derleyin ve çalıştırın.

## 📱 Ekran Görüntüleri

*(Buraya uygulamanın ekran görüntülerini ekleyebilirsiniz)*

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) ile lisanslanmıştır.
