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
