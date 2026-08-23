# بناء تطبيق الهاتف v13 عبر GitHub

1. ارفع محتويات المشروع إلى المستودع نفسه.
2. تأكد أن الملف `.github/workflows/build-apk.yml` موجود.
3. افتح **Actions** ثم **Build Android APK**.
4. اختر **Run workflow**.
5. بعد ظهور علامة النجاح الخضراء، افتح التشغيل وانزل إلى **Artifacts**.
6. نزّل **CarInspectionApp-APK**، فك الضغط، وثبّت `app-debug.apk` على الهاتف.

# بناء APK عبر GitHub بدون Android Studio

## 1) أنشئ مستودع GitHub جديد
- افتح github.com وسجّل الدخول.
- اضغط New repository.
- اكتب اسمًا مثل: CarInspectionApp.
- اختر Public أو Private.
- اضغط Create repository.

## 2) ارفع ملفات المشروع
مهم: ارفع محتويات مجلد CarInspectionApp نفسه، بحيث يظهر في الصفحة الرئيسية للمستودع:
- app
- .github
- build.gradle
- settings.gradle
- gradle.properties
- README.md

لا ترفع المجلد الأب بحيث تصبح الملفات داخل CarInspectionApp/CarInspectionApp.

## 3) شغّل بناء APK
- افتح تبويب Actions في GitHub.
- اختر Build Android APK.
- اضغط Run workflow.
- اضغط الزر الأخضر Run workflow.

## 4) انتظر اكتمال البناء
عادة يستغرق عدة دقائق.
عندما تظهر علامة صح خضراء، افتح عملية البناء.

## 5) نزّل APK
في أسفل الصفحة ستجد قسم Artifacts.
اضغط:
CarInspectionApp-APK

سينزل ملف ZIP. فك الضغط وستجد:
app-debug.apk

انقل app-debug.apk إلى هاتف Android وثبته.

## إذا ظهر خطأ
صوّر صفحة الخطأ في GitHub Actions وأرسلها في المحادثة.


## تحديث v9
- الحفاظ على معايرة v7 الناجحة للصفحات الثانية والثالثة.
- ضبط بيانات السيارة في أعلى الصفحة الأولى داخل المربعات، مع تصغير تلقائي للنص الطويل بدل اقتطاعه.
- اعتماد 18 إشارة طبلون مستقلة (3 أعمدة × 6 صفوف) وربط كل إشارة بمربعها المقابل في PDF.
