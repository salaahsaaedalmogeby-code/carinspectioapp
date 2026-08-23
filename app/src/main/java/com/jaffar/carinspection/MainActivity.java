package com.jaffar.carinspection;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Environment;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.security.MessageDigest;

public class MainActivity extends Activity {

    private static final int REQ_ENGINE_CAMERA = 1201;
    private static final int REQ_CAR_PHOTO_BASE = 1300;
    private static final String AUTH_PREFS = "system_auth";
    private static final String PASSWORD_HASH_KEY = "password_hash_v1";
    private LinearLayout form;
    private Button btnSave, btnPdf;
    private ImageView enginePhotoPreview;
    private TextView enginePhotoStatus;
    private Map<String, EditText> textFields = new LinkedHashMap<>();
    private Map<String, Spinner> choiceFields = new LinkedHashMap<>();
    private String currentInspectionId = null;
    private String enginePhotoPath = "";
    private String pendingPhotoPath = "";
    private final String[] carPhotoPaths = new String[]{"", "", "", ""};
    private final String[] pendingCarPhotoPaths = new String[]{"", "", "", ""};
    private final ImageView[] carPhotoPreviews = new ImageView[4];
    private final TextView[] carPhotoStatuses = new TextView[4];

    final String[] condition4 = {"اختر", "ممتاز", "جيد", "متوسط", "ضعيف"};
    final String[] yesNo = {"اختر", "يوجد", "لا يوجد"};
    final String[] goodBad = {"اختر", "جيد", "غير جيد"};
    final String[] intact = {"اختر", "سليم", "غير سليم"};
    final String[] bodyBase = {"اختر", "سليم", "رش خفيف", "رش كبير", "سمكرة خفيف", "سمكرة كبير"};
    final String[] bodyPaint = {"اختر", "سليم", "مقلوب كامل", "قلبة جانبية", "مرشوش كامل", "رشة جزئية (تلقيطات)"};
    final String[] transmissionType = {"اختر", "عادي", "أوتوماتيك"};
    final String[] transferType = {"اختر", "ذاتي", "يدوي"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        form = findViewById(R.id.formContainer);
        btnSave = findViewById(R.id.btnSave);
        btnPdf = findViewById(R.id.btnPdf);

        findViewById(R.id.btnNew).setOnClickListener(v -> newInspection());
        findViewById(R.id.btnSaved).setOnClickListener(v -> showSavedInspections());
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> changePassword());
        btnSave.setOnClickListener(v -> saveInspection(true));
        btnPdf.setOnClickListener(v -> generatePdf());

        // لا نعرض بيانات النظام قبل نجاح تسجيل الدخول.
        findViewById(R.id.content).setVisibility(View.INVISIBLE);
        showAuthenticationDialog();
    }

    private TextView section(String title) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(19);
        t.setTextColor(Color.rgb(16,42,67));
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(8, 22, 8, 10);
        t.setGravity(Gravity.RIGHT);
        return t;
    }

    private EditText addText(String key, String hint) {
        TextView label = new TextView(this);
        label.setText(hint);
        label.setTextSize(15);
        label.setPadding(4, 8, 4, 2);
        form.addView(label);
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setGravity(Gravity.RIGHT);
        e.setSingleLine(!hint.contains("ملاحظ") && !hint.contains("التجربة"));
        if (!e.isSingleLine()) e.setMinLines(3);
        form.addView(e, new LinearLayout.LayoutParams(-1, -2));
        textFields.put(key, e);
        return e;
    }

    private Spinner addChoice(String key, String labelText, String[] options) {
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(15);
        label.setPadding(4, 8, 4, 2);
        form.addView(label);
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        s.setAdapter(a);
        form.addView(s, new LinearLayout.LayoutParams(-1, -2));
        choiceFields.put(key, s);
        return s;
    }

    private void newInspection() {
        currentInspectionId = null;
        enginePhotoPath = "";
        pendingPhotoPath = "";
        Arrays.fill(carPhotoPaths, "");
        Arrays.fill(pendingCarPhotoPaths, "");
        buildForm();
    }

    private void buildForm() {
        form.removeAllViews();
        textFields.clear();
        choiceFields.clear();
        form.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.VISIBLE);
        btnPdf.setVisibility(View.VISIBLE);

        form.addView(section("1) بيانات السيارة"));
        addText("owner", "اسم مالك السيارة");
        addText("manufacturer", "الشركة المصنعة");
        addText("vehicle_type", "نوع المركبة");
        addText("model", "الموديل");
        addText("color", "اللون");
        addText("vin", "رقم الهيكل");
        addText("engine_type", "نوع المحرك");
        addText("plate", "رقم اللوحة");
        addText("drive", "الدفع");

        form.addView(section("2) فحص الهيكل والبودي"));
        addChoice("base", "القاعدة", bodyBase);
        addChoice("body", "البودي", bodyPaint);
        addText("base_notes", "ملاحظات القاعدة");
        addText("body_notes", "ملاحظات البودي");
        addText("general_notes", "ملاحظات أخرى - نهاية الصفحة الأولى");

        form.addView(section("3) الأجزاء الداخلية للسيارة"));
        String[] interior = {"الزجاجات","فتحة السقف","مقابض الأبواب","الإضاءة والأنوار","الإشارات (الاصطبات)","الديكورات","الشنطة الخلفية","الأبواب الخلفية","الشاشة أو المسجل","المرايات","المساحات","المقاعد","الطبلون"};
        for (String x : interior) addChoice("int_"+x, x, intact);

        form.addView(section("4) فحص المحرك"));
        addEngineCameraControls();
        addText("engine_kind", "نوع المحرك");
        addChoice("engine_seal", "وضع المحرك", new String[]{"اختر","مختوم","مفكوك"});
        addChoice("engine_consumption", "صرفية المحرك", yesNo);
        addChoice("engine_sound", "أصوات في المحرك", yesNo);
        addChoice("engine_leaks", "تهريبات في المحرك", yesNo);
        addChoice("engine_perf", "أداء المحرك", condition4);
        addChoice("engine_temp", "حرارة المحرك", new String[]{"اختر","طبيعي","توجد حرارة"});
        addChoice("engine_smoke", "دخان أسود", yesNo);
        addChoice("engine_vibration", "رجفة في المحرك", yesNo);
        addText("engine_notes", "ملاحظات المحرك");

        form.addView(section("5) فحص ناقل الحركة"));
        addChoice("trans_type", "نوع ناقل الحركة", transmissionType);
        addChoice("clutch", "حالة الاسبيت", new String[]{"اختر","مختوم","مفكوك"});
        addChoice("shifting", "وضع التغييرات (التعشيق)", goodBad);
        addChoice("trans_sound", "أصوات في الاسبيت", yesNo);
        addChoice("trans_leaks", "تهريبات في الاسبيت", yesNo);
        addChoice("trans_perf", "أداء ناقل الحركة", condition4);
        addText("trans_notes", "ملاحظات ناقل الحركة");

        form.addView(section("6) فحص الدبل"));
        addChoice("transfer_type", "نوع الدبل", transferType);
        addChoice("transfer_seal", "حالة الدبل", new String[]{"اختر","مختوم","مفكوك"});
        addChoice("transfer_shift", "وضع التغييرات (التعشيق)", goodBad);
        addChoice("transfer_sound", "أصوات في الدبل", yesNo);
        addChoice("transfer_leaks", "تهريبات في الدبل", yesNo);
        addChoice("shaft", "حالة صرة الدوران", goodBad);
        addText("transfer_notes", "ملاحظات الدبل");

        form.addView(section("7) فحص الدفرنس"));
        addChoice("front_diff_state", "الدفرنس الأمامي - الحالة", goodBad);
        addChoice("front_diff_gears", "الدفرنس الأمامي - وضع الكوارين", goodBad);
        addChoice("front_diff_sound", "الدفرنس الأمامي - أصوات", yesNo);
        addChoice("front_diff_leaks", "الدفرنس الأمامي - تهريبات", yesNo);
        addChoice("front_axles", "الدفرنس الأمامي - صلب العكوس", goodBad);
        addChoice("rear_diff_state", "الدفرنس الخلفي - الحالة", goodBad);
        addChoice("rear_diff_gears", "الدفرنس الخلفي - وضع الكوارين", goodBad);
        addChoice("rear_diff_sound", "الدفرنس الخلفي - أصوات", yesNo);
        addChoice("rear_diff_leaks", "الدفرنس الخلفي - تهريبات", yesNo);
        addChoice("rear_axles", "الدفرنس الخلفي - صلب العكوس", goodBad);
        addText("diff_notes", "ملاحظات الدفرنس");
        addChoice("ac_status", "نظام التكييف والتبريد", new String[]{"اختر","يعمل","لا يعمل"});

        form.addView(section("8) التوجيه ونظام التعليق"));
        String[] suspension = {"مجموعة الدركسون","الذراعات","المقصات","المساعدات","الكعكات","الفرامل","السبرنجه","عمود التوازن"};
        for (String x : suspension) addChoice("sus_"+x, x, goodBad);
        addText("suspension_notes", "ملاحظات التوجيه والتعليق");

        form.addView(section("9) حالة الإطارات"));
        addChoice("tire_fl", "الإطار الأمامي الأيسر", new String[]{"اختر","جيد","تالف"});
        addChoice("tire_fr", "الإطار الأمامي الأيمن", new String[]{"اختر","جيد","تالف"});
        addChoice("tire_rl", "الإطار الخلفي الأيسر", new String[]{"اختر","جيد","تالف"});
        addChoice("tire_rr", "الإطار الخلفي الأيمن", new String[]{"اختر","جيد","تالف"});

        form.addView(section("10) التجربة الميدانية للسيارة"));
        addText("road_test", "نتيجة التجربة الميدانية");

        form.addView(section("11) فحص الكمبيوتر وإشارات الطبلون (18 إشارة)"));
        // القالب النهائي يحتوي على 18 رمزاً مستقلاً (3 أعمدة × 6 صفوف).
        // لكل رمز اختيار مستقل حتى لا تُهمل أي إشارة موجودة في التقرير الورقي.
        String[] lights = {
                "4WD", "Brake", "صيانة/Record maint", "T-BELT", "مفتاح/Immobilizer", "تحذير الدركسون",
                "حرارة المحرك", "شمعات التسخين/Glow Plug", "ABS", "ضغط زيت المحرك", "حرارة سائل التبريد", "ESP",
                "Check Engine", "مستوى زيت المحرك", "مانع الانزلاق/Traction", "بطارية", "Airbag", "ضغط الإطارات"
        };
        for (int i=0; i<lights.length; i++) {
            String x = lights[i];
            addChoice("light_"+x, (i+1) + ") " + x, new String[]{"اختر","لا توجد إشارة","توجد إشارة"});
        }

        form.addView(section("12) صور السيارة - اختيارية (حتى 4 صور)"));
        addCarPhotosControls();

        form.addView(section("13) بيانات التقرير"));
        addText("inspector", "اسم المهندس / الفاحص المختص");
        refreshEnginePhotoPreview();
    }


    private void showAuthenticationDialog() {
        final boolean firstRun = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                .getString(PASSWORD_HASH_KEY, "").isEmpty();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);

        EditText pass1 = new EditText(this);
        pass1.setHint(firstRun ? "أنشئ كلمة مرور (4 أحرف/أرقام على الأقل)" : "أدخل كلمة المرور");
        pass1.setSingleLine(true);
        pass1.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(pass1);

        EditText pass2 = null;
        if (firstRun) {
            pass2 = new EditText(this);
            pass2.setHint("تأكيد كلمة المرور");
            pass2.setSingleLine(true);
            pass2.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            box.addView(pass2);
        }
        final EditText confirmField = pass2;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(firstRun ? "إعداد كلمة مرور النظام" : "دخول نظام الفحص")
                .setMessage(firstRun ? "أول تشغيل: أنشئ كلمة مرور خاصة بالنظام. يمكنك تغييرها لاحقاً من زر تغيير كلمة المرور." : "أدخل كلمة المرور لفتح النظام.")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton(firstRun ? "حفظ والدخول" : "دخول", null)
                .setNegativeButton("إغلاق", (d,w) -> finish())
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String p1 = pass1.getText().toString();
            if (p1.length() < 4) {
                pass1.setError("كلمة المرور يجب أن تكون 4 أحرف/أرقام على الأقل");
                return;
            }
            if (firstRun) {
                String p2 = confirmField == null ? "" : confirmField.getText().toString();
                if (!p1.equals(p2)) {
                    if (confirmField != null) confirmField.setError("تأكيد كلمة المرور غير مطابق");
                    return;
                }
                getSharedPreferences(AUTH_PREFS, MODE_PRIVATE).edit()
                        .putString(PASSWORD_HASH_KEY, hashPassword(p1)).apply();
            } else {
                String saved = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                        .getString(PASSWORD_HASH_KEY, "");
                if (!hashPassword(p1).equals(saved)) {
                    pass1.setError("كلمة المرور غير صحيحة");
                    pass1.setText("");
                    return;
                }
            }
            dialog.dismiss();
            findViewById(R.id.content).setVisibility(View.VISIBLE);
            newInspection();
        }));
        dialog.show();
    }

    private void changePassword() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);
        EditText current = passwordField("كلمة المرور الحالية");
        EditText next = passwordField("كلمة المرور الجديدة");
        EditText confirm = passwordField("تأكيد كلمة المرور الجديدة");
        box.addView(current); box.addView(next); box.addView(confirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("تغيير كلمة المرور")
                .setView(box)
                .setPositiveButton("حفظ", null)
                .setNegativeButton("إلغاء", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String saved = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE).getString(PASSWORD_HASH_KEY, "");
            if (!hashPassword(current.getText().toString()).equals(saved)) {
                current.setError("كلمة المرور الحالية غير صحيحة"); return;
            }
            if (next.getText().length() < 4) {
                next.setError("4 أحرف/أرقام على الأقل"); return;
            }
            if (!next.getText().toString().equals(confirm.getText().toString())) {
                confirm.setError("تأكيد كلمة المرور غير مطابق"); return;
            }
            getSharedPreferences(AUTH_PREFS, MODE_PRIVATE).edit()
                    .putString(PASSWORD_HASH_KEY, hashPassword(next.getText().toString())).apply();
            dialog.dismiss();
            Toast.makeText(this, "تم تغيير كلمة المرور بنجاح", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private EditText passwordField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }

    private String hashPassword(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(value.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format(Locale.US, "%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private void addCarPhotosControls() {
        TextView note = new TextView(this);
        note.setText("هذه الصور اختيارية. إذا لم تُلتقط أي صورة فلن تُضاف الصفحة الرابعة إلى التقرير.");
        note.setTextSize(14);
        note.setGravity(Gravity.RIGHT);
        note.setPadding(4, 4, 4, 10);
        form.addView(note);

        for (int i=0; i<4; i++) {
            final int index = i;
            TextView title = new TextView(this);
            title.setText("صورة السيارة " + (i+1));
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.RIGHT);
            title.setPadding(4, 10, 4, 4);
            form.addView(title);

            ImageView preview = new ImageView(this);
            preview.setAdjustViewBounds(true);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setBackgroundColor(Color.rgb(230,234,238));
            carPhotoPreviews[i] = preview;
            form.addView(preview, new LinearLayout.LayoutParams(-1, dp(150)));

            TextView status = new TextView(this);
            status.setGravity(Gravity.CENTER);
            carPhotoStatuses[i] = status;
            form.addView(status);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Button capture = new Button(this);
            capture.setText("تصوير");
            capture.setOnClickListener(v -> captureCarPhoto(index));
            row.addView(capture, new LinearLayout.LayoutParams(0, -2, 1));
            Button remove = new Button(this);
            remove.setText("حذف");
            remove.setOnClickListener(v -> {
                carPhotoPaths[index] = "";
                pendingCarPhotoPaths[index] = "";
                refreshCarPhotoPreview(index);
                saveInspection(false);
            });
            row.addView(remove, new LinearLayout.LayoutParams(0, -2, 1));
            form.addView(row);
            refreshCarPhotoPreview(index);
        }
    }

    private void captureCarPhoto(int index) {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "لا يوجد تطبيق كاميرا متاح", Toast.LENGTH_LONG).show();
                return;
            }
            File dir = new File(getFilesDir(), "car_photos");
            if (!dir.exists()) dir.mkdirs();
            File photo = new File(dir, "car_" + (index+1) + "_" + System.currentTimeMillis() + ".jpg");
            pendingCarPhotoPaths[index] = photo.getAbsolutePath();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.setClipData(ClipData.newRawUri("car_photo", uri));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            for (android.content.pm.ResolveInfo ri : getPackageManager().queryIntentActivities(intent, 0)) {
                grantUriPermission(ri.activityInfo.packageName, uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivityForResult(intent, REQ_CAR_PHOTO_BASE + index);
        } catch (Exception e) {
            showError("تعذر فتح الكاميرا", e);
        }
    }

    private void refreshCarPhotoPreview(int index) {
        if (index < 0 || index >= 4 || carPhotoPreviews[index] == null) return;
        String path = carPhotoPaths[index];
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            Bitmap b = decodeScaledBitmap(path, 900, 600);
            carPhotoPreviews[index].setImageBitmap(b);
            carPhotoStatuses[index].setText("تم حفظ الصورة ✓");
        } else {
            carPhotoPreviews[index].setImageDrawable(null);
            carPhotoPreviews[index].setBackgroundColor(Color.rgb(230,234,238));
            carPhotoStatuses[index].setText("لم يتم التصوير");
        }
    }

    private boolean hasCarPhotos() {
        for (String p : carPhotoPaths) if (p != null && !p.isEmpty() && new File(p).exists()) return true;
        return false;
    }

    private void addEngineCameraControls() {
        TextView note = new TextView(this);
        note.setText("صورة المحرك: تُلتقط أثناء الفحص وتظهر داخل التقرير في الصفحة الأولى.");
        note.setTextSize(14);
        note.setGravity(Gravity.RIGHT);
        note.setPadding(4, 4, 4, 8);
        form.addView(note);

        enginePhotoPreview = new ImageView(this);
        enginePhotoPreview.setAdjustViewBounds(true);
        enginePhotoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(-1, dp(190));
        form.addView(enginePhotoPreview, imgLp);

        enginePhotoStatus = new TextView(this);
        enginePhotoStatus.setGravity(Gravity.CENTER);
        enginePhotoStatus.setPadding(4,6,4,6);
        form.addView(enginePhotoStatus);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        Button capture = new Button(this);
        capture.setText("تصوير المحرك بالكاميرا");
        capture.setOnClickListener(v -> captureEnginePhoto());
        row.addView(capture, new LinearLayout.LayoutParams(0, -2, 1));
        Button remove = new Button(this);
        remove.setText("حذف الصورة");
        remove.setOnClickListener(v -> {
            enginePhotoPath = "";
            pendingPhotoPath = "";
            refreshEnginePhotoPreview();
        });
        row.addView(remove, new LinearLayout.LayoutParams(0, -2, 1));
        form.addView(row);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void captureEnginePhoto() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "لا يوجد تطبيق كاميرا متاح", Toast.LENGTH_LONG).show();
                return;
            }
            File dir = new File(getFilesDir(), "engine_photos");
            if (!dir.exists()) dir.mkdirs();
            File photo = new File(dir, "engine_" + System.currentTimeMillis() + ".jpg");
            pendingPhotoPath = photo.getAbsolutePath();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.setClipData(ClipData.newRawUri("engine_photo", uri));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            for (android.content.pm.ResolveInfo ri : getPackageManager().queryIntentActivities(intent, 0)) {
                grantUriPermission(ri.activityInfo.packageName, uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivityForResult(intent, REQ_ENGINE_CAMERA);
        } catch (Exception e) {
            showError("تعذر فتح الكاميرا", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ENGINE_CAMERA) {
            if (resultCode == RESULT_OK && pendingPhotoPath != null && !pendingPhotoPath.isEmpty()) {
                enginePhotoPath = pendingPhotoPath;
                refreshEnginePhotoPreview();
                saveInspection(false);
            } else if (pendingPhotoPath != null && !pendingPhotoPath.isEmpty()) {
                new File(pendingPhotoPath).delete();
            }
            pendingPhotoPath = "";
            return;
        }
        if (requestCode >= REQ_CAR_PHOTO_BASE && requestCode < REQ_CAR_PHOTO_BASE + 4) {
            int index = requestCode - REQ_CAR_PHOTO_BASE;
            String pending = pendingCarPhotoPaths[index];
            if (resultCode == RESULT_OK && pending != null && !pending.isEmpty()) {
                carPhotoPaths[index] = pending;
                refreshCarPhotoPreview(index);
                saveInspection(false);
            } else if (pending != null && !pending.isEmpty()) {
                new File(pending).delete();
            }
            pendingCarPhotoPaths[index] = "";
        }
    }

    private void refreshEnginePhotoPreview() {
        if (enginePhotoPreview == null) return;
        if (enginePhotoPath != null && !enginePhotoPath.isEmpty() && new File(enginePhotoPath).exists()) {
            Bitmap b = decodeScaledBitmap(enginePhotoPath, 900, 600);
            enginePhotoPreview.setImageBitmap(b);
            enginePhotoStatus.setText("تم حفظ صورة المحرك ✓ — يمكنك إعادة التصوير في أي وقت");
        } else {
            enginePhotoPreview.setImageDrawable(null);
            enginePhotoPreview.setBackgroundColor(Color.rgb(230,234,238));
            enginePhotoStatus.setText("لم يتم تصوير المحرك بعد");
        }
    }

    private String value(String key) {
        if (textFields.containsKey(key)) return textFields.get(key).getText().toString().trim();
        if (choiceFields.containsKey(key)) return choiceFields.get(key).getSelectedItem().toString();
        return "";
    }

    private JSONObject collectData() throws Exception {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, EditText> e : textFields.entrySet()) obj.put("t_"+e.getKey(), e.getValue().getText().toString());
        for (Map.Entry<String, Spinner> e : choiceFields.entrySet()) obj.put("s_"+e.getKey(), e.getValue().getSelectedItem().toString());
        obj.put("engine_photo", enginePhotoPath == null ? "" : enginePhotoPath);
        for (int i=0; i<4; i++) obj.put("car_photo_"+i, carPhotoPaths[i] == null ? "" : carPhotoPaths[i]);
        obj.put("saved_at", new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date()));
        return obj;
    }

    private String saveInspection(boolean showToast) {
        try {
            if (currentInspectionId == null || currentInspectionId.isEmpty()) {
                currentInspectionId = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
            }
            JSONObject obj = collectData();
            obj.put("id", currentInspectionId);
            getSharedPreferences("inspection_records", MODE_PRIVATE).edit()
                    .putString("record_"+currentInspectionId, obj.toString()).apply();
            addRecordId(currentInspectionId);
            if (showToast) Toast.makeText(this, "تم حفظ الفحص ويمكن تعديله لاحقاً", Toast.LENGTH_SHORT).show();
            return currentInspectionId;
        } catch (Exception e) {
            showError("تعذر حفظ الفحص", e);
            return null;
        }
    }

    private void addRecordId(String id) {
        SharedPreferences sp = getSharedPreferences("inspection_records", MODE_PRIVATE);
        String raw = sp.getString("record_ids", "");
        List<String> ids = new ArrayList<>();
        if (!raw.isEmpty()) ids.addAll(Arrays.asList(raw.split(",")));
        ids.remove(id);
        ids.add(0, id);
        StringBuilder sb = new StringBuilder();
        for (String x : ids) { if (x.trim().isEmpty()) continue; if (sb.length()>0) sb.append(','); sb.append(x); }
        sp.edit().putString("record_ids", sb.toString()).apply();
    }

    private List<String> getRecordIds() {
        String raw = getSharedPreferences("inspection_records", MODE_PRIVATE).getString("record_ids", "");
        List<String> ids = new ArrayList<>();
        if (!raw.isEmpty()) for (String x : raw.split(",")) if (!x.trim().isEmpty()) ids.add(x);
        return ids;
    }

    private void showSavedInspections() {
        try {
            List<String> ids = getRecordIds();
            if (ids.isEmpty()) { Toast.makeText(this, "لا توجد فحوصات محفوظة", Toast.LENGTH_SHORT).show(); return; }
            SharedPreferences sp = getSharedPreferences("inspection_records", MODE_PRIVATE);
            String[] labels = new String[ids.size()];
            for (int i=0;i<ids.size();i++) {
                JSONObject o = new JSONObject(sp.getString("record_"+ids.get(i), "{}"));
                String owner = o.optString("t_owner", "بدون اسم");
                String plate = o.optString("t_plate", "");
                String date = o.optString("saved_at", "");
                labels[i] = owner + (plate.isEmpty()?"":" — " + plate) + "\n" + date;
            }
            new AlertDialog.Builder(this)
                    .setTitle("الفحوصات المحفوظة — اختر فحصاً للتعديل")
                    .setItems(labels, (d, which) -> loadInspection(ids.get(which)))
                    .setNegativeButton("إلغاء", null).show();
        } catch (Exception e) { showError("تعذر فتح الفحوصات المحفوظة", e); }
    }

    private void loadInspection(String id) {
        try {
            String raw = getSharedPreferences("inspection_records", MODE_PRIVATE).getString("record_"+id, null);
            if (raw == null) return;
            currentInspectionId = id;
            JSONObject o = new JSONObject(raw);
            buildForm();
            for (Map.Entry<String, EditText> e : textFields.entrySet()) e.getValue().setText(o.optString("t_"+e.getKey(), ""));
            for (Map.Entry<String, Spinner> e : choiceFields.entrySet()) {
                String val = o.optString("s_"+e.getKey(), "اختر");
                Spinner s = e.getValue();
                for (int i=0;i<s.getCount();i++) if (s.getItemAtPosition(i).toString().equals(val)) { s.setSelection(i); break; }
            }
            enginePhotoPath = o.optString("engine_photo", "");
            for (int i=0; i<4; i++) carPhotoPaths[i] = o.optString("car_photo_"+i, "");
            refreshEnginePhotoPreview();
            for (int i=0; i<4; i++) refreshCarPhotoPreview(i);
            Toast.makeText(this, "تم فتح الفحص — عدّل البيانات ثم اضغط حفظ الفحص", Toast.LENGTH_LONG).show();
        } catch (Exception e) { showError("تعذر تحميل الفحص", e); }
    }

    private void generatePdf() {
        PdfDocument outDoc = null;
        PdfRenderer renderer = null;
        ParcelFileDescriptor pfd = null;
        try {
            if (saveInspection(false) == null) return;
            File template = copyTemplateToCache();
            pfd = ParcelFileDescriptor.open(template, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
            outDoc = new PdfDocument();

            int lastPageW = 595;
            int lastPageH = 842;
            for (int i=0; i<renderer.getPageCount(); i++) {
                PdfRenderer.Page rp = renderer.openPage(i);
                int pageW = rp.getWidth();
                int pageH = rp.getHeight();
                lastPageW = pageW; lastPageH = pageH;
                int renderScale = 2;
                Bitmap bg = Bitmap.createBitmap(pageW*renderScale, pageH*renderScale, Bitmap.Config.ARGB_8888);
                bg.eraseColor(Color.WHITE);
                rp.render(bg, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                rp.close();

                PdfDocument.Page op = outDoc.startPage(new PdfDocument.PageInfo.Builder(pageW, pageH, i+1).create());
                Canvas c = op.getCanvas();
                c.drawBitmap(bg, null, new RectF(0,0,pageW,pageH), null);
                bg.recycle();
                if (i == 0) overlayPage1(c, pageW, pageH);
                if (i == 1) overlayPage2(c, pageW, pageH);
                if (i == 2) overlayPage3(c, pageW, pageH);
                outDoc.finishPage(op);
            }
            if (hasCarPhotos()) addCarPhotosPage(outDoc, lastPageW, lastPageH, renderer.getPageCount()+1);

            String plate = safeName(value("plate"));
            if (plate.isEmpty()) plate = "car";
            String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "Inspection_" + plate + "_" + time + ".pdf";
            String savedPath = savePdfToDownloads(outDoc, fileName);
            outDoc.close(); outDoc = null;

            new AlertDialog.Builder(this)
                    .setTitle("تم إنشاء التقرير")
                    .setMessage("تم إنشاء التقرير بنفس قالب الفحص الأصلي وحفظه في:\n" + savedPath + "\n\nيمكنك تعديل هذا الفحص لاحقاً من زر: الفحوصات المحفوظة.")
                    .setPositiveButton("حسناً", null).show();
        } catch (Exception e) {
            showError("تعذر إنشاء التقرير", e);
        } finally {
            try { if (outDoc != null) outDoc.close(); } catch(Exception ignored) {}
            try { if (renderer != null) renderer.close(); } catch(Exception ignored) {}
            try { if (pfd != null) pfd.close(); } catch(Exception ignored) {}
        }
    }

    private File copyTemplateToCache() throws Exception {
        File f = new File(getCacheDir(), "inspection_template.pdf");
        InputStream in = getResources().openRawResource(R.raw.inspection_template);
        FileOutputStream out = new FileOutputStream(f);
        byte[] buf = new byte[8192]; int n;
        while ((n=in.read(buf))>0) out.write(buf,0,n);
        in.close(); out.close();
        return f;
    }

    private String savePdfToDownloads(PdfDocument doc, String fileName) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/تقارير فحص السيارات");
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("تعذر إنشاء ملف في مجلد التنزيلات");
            OutputStream out = getContentResolver().openOutputStream(uri);
            if (out == null) throw new IOException("تعذر فتح ملف التقرير");
            doc.writeTo(out); out.close();
            return "Download/تقارير فحص السيارات/" + fileName;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "تقارير فحص السيارات");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("تعذر إنشاء مجلد التقارير");
            File file = new File(dir, fileName);
            FileOutputStream out = new FileOutputStream(file);
            doc.writeTo(out); out.close();
            return file.getAbsolutePath();
        }
    }

    // v13: new notes-box template + automatic date + password protection + optional 4-photo page.
    private void overlayPage1(Canvas c, int w, int h) {
        Paint text = pdfPaint(10.5f);
        Paint mark = pdfPaint(10.5f);
        String date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date());
        drawCentered(c,text,date,516,195,105);
        drawRtl(c,pdfPaint(11f),value("owner"), 420, 233, 360);

        // بيانات السيارة — v9: مراكز الخانات البيضاء مقاسة من القالب النهائي نفسه.
        // الصف الأول: الشركة | نوع المركبة | الموديل | اللون.
        // الصف الثاني: رقم الهيكل | نوع المحرك | رقم اللوحة | الدفع.
        // خط الأساس مرفوع داخل كل خلية حتى لا يلامس العنوان الأصفر أو الحد السفلي.
        final float vehicleRow1Y = 259.5f;
        final float vehicleRow2Y = 280.0f;
        drawCenteredAutoFit(c,value("manufacturer"), 422.5f, vehicleRow1Y, 66, 11.5f, 6.4f);
        drawCenteredAutoFit(c,value("vehicle_type"), 286.2f, vehicleRow1Y, 50, 11.5f, 6.4f);
        drawCenteredAutoFit(c,value("model"), 164.6f, vehicleRow1Y, 43, 11.5f, 6.4f);
        drawCenteredAutoFit(c,value("color"), 60.7f, vehicleRow1Y, 57, 11.5f, 6.4f);
        drawCenteredAutoFit(c,value("vin"), 422.5f, vehicleRow2Y, 66, 11.0f, 5.6f);
        drawCenteredAutoFit(c,value("engine_type"), 286.2f, vehicleRow2Y, 50, 11.5f, 6.0f);
        drawCenteredAutoFit(c,value("plate"), 164.6f, vehicleRow2Y, 43, 11.0f, 5.8f);
        drawCenteredAutoFit(c,value("drive"), 60.7f, vehicleRow2Y, 57, 11.0f, 5.8f);

        if (enginePhotoPath != null && !enginePhotoPath.isEmpty() && new File(enginePhotoPath).exists()) {
            Bitmap photo = decodeScaledBitmap(enginePhotoPath, 1600, 1200);
            if (photo != null) {
                RectF box = new RectF(196, 313, 558, 583);
                drawBitmapCenterCrop(c, photo, box);
                photo.recycle();
            }
        }

        // الأجزاء الداخلية: مركز خلية جيد/سيئ، لا فوق الكلمة.
        String[] interior = {"الزجاجات","فتحة السقف","مقابض الأبواب","الإضاءة والأنوار","الإشارات (الاصطبات)","الديكورات","الشنطة الخلفية","الأبواب الخلفية","الشاشة أو المسجل","المرايات","المساحات","المقاعد","الطبلون"};
        float y = 376.5f;
        for (String x : interior) {
            String v = value("int_"+x);
            if ("سليم".equals(v)) tick(c,mark,89.5f,y);
            else if ("غير سليم".equals(v)) tick(c,mark,50.5f,y);
            y += 16.55f;
        }

        // القاعدة: الإجابة في الصف الأبيض تحت اسم الاختيار.
        String base = value("base");
        if ("سليم".equals(base)) tick(c,mark,482,627);
        else if ("رش كبير".equals(base)) tick(c,mark,443,627);
        else if ("رش خفيف".equals(base)) tick(c,mark,406.5f,627);
        else if ("سمكرة كبير".equals(base)) tick(c,mark,366.5f,627);
        else if ("سمكرة خفيف".equals(base)) tick(c,mark,323,627);
        // ملاحظات القاعدة تبدأ على نفس سطر "ملاحظات أخرى" وفي المساحة الفارغة يساره.
        drawWrappedRtl(c,pdfPaint(8.5f),value("base_notes"),225,599,185,11,3);

        String body = value("body");
        if ("سليم".equals(body)) tick(c,mark,482,676);
        else if ("مقلوب كامل".equals(body)) tick(c,mark,443,676);
        else if ("قلبة جانبية".equals(body)) tick(c,mark,406.5f,676);
        else if ("مرشوش كامل".equals(body)) tick(c,mark,366.5f,676);
        else if ("رشة جزئية (تلقيطات)".equals(body)) tick(c,mark,323,676);
        drawWrappedRtl(c,pdfPaint(8.5f),value("body_notes"),225,648,185,11,3);

        // الملاحظات العامة أسفل الجدولين وعلى السطر المخصص لها.
        drawWrappedRtl(c,pdfPaint(9.5f),value("general_notes"),548,732,485,13,2);
    }

    private void overlayPage2(Canvas c, int w, int h) {
        Paint m = pdfPaint(9.5f);
        Paint t = pdfPaint(9.5f);
        final float engineY = 135.5f;

        // نوع المحرك كتابة فقط داخل الخانة الواقعة تحت "نوع المحرك".
        drawCentered(c,t,value("engine_kind"),526,135.5f,36);
        // بقية فحص المحرك: ✓ في مركز خلية الإجابة البيضاء تحت كل اختيار.
        tickChoice(c,m,value("engine_seal"),engineY,new String[]{"مختوم","مفكوك"},new float[]{495,469.5f});
        tickChoice(c,m,value("engine_consumption"),engineY,new String[]{"يوجد","لا يوجد"},new float[]{442,415.5f});
        tickChoice(c,m,value("engine_sound"),engineY,new String[]{"يوجد","لا يوجد"},new float[]{389.5f,361.5f});
        tickChoice(c,m,value("engine_leaks"),engineY,new String[]{"يوجد","لا يوجد"},new float[]{332.5f,305.5f});
        tickChoice(c,m,value("engine_perf"),engineY,new String[]{"ممتاز","جيد","متوسط","ضعيف"},new float[]{279.5f,256,231.5f,203.5f});
        tickChoice(c,m,value("engine_temp"),engineY,new String[]{"طبيعي","توجد حرارة"},new float[]{177,143});
        tickChoice(c,m,value("engine_smoke"),engineY,new String[]{"يوجد","لا يوجد"},new float[]{106.5f,78});
        tickChoice(c,m,value("engine_vibration"),engineY,new String[]{"يوجد","لا يوجد"},new float[]{53.5f,29.5f});
        drawWrappedRtl(c,pdfPaint(9f),value("engine_notes"),510,151,480,12,7);

        final float transY = 328f;
        tickChoice(c,m,value("trans_type"),transY,new String[]{"أوتوماتيك","عادي"},new float[]{523.5f,482.5f});
        tickChoice(c,m,value("clutch"),transY,new String[]{"مختوم","مفكوك"},new float[]{441,398.5f});
        tickChoice(c,m,value("shifting"),transY,new String[]{"جيد","غير جيد"},new float[]{359.5f,322.5f});
        tickChoice(c,m,value("trans_sound"),transY,new String[]{"يوجد","لا يوجد"},new float[]{287,253});
        tickChoice(c,m,value("trans_leaks"),transY,new String[]{"يوجد","لا يوجد"},new float[]{219,183});
        tickChoice(c,m,value("trans_perf"),transY,new String[]{"ممتاز","جيد","متوسط","ضعيف"},new float[]{147.5f,114.5f,79,40.5f});
        drawWrappedRtl(c,pdfPaint(9f),value("trans_notes"),510,346,475,12,5);

        final float transferY = 486f;
        tickChoice(c,m,value("transfer_type"),transferY,new String[]{"ذاتي","يدوي"},new float[]{517.5f,476.5f});
        tickChoice(c,m,value("transfer_seal"),transferY,new String[]{"مختوم","مفكوك"},new float[]{432.5f,392.5f});
        tickChoice(c,m,value("transfer_shift"),transferY,new String[]{"جيد","غير جيد"},new float[]{353.5f,316.5f});
        tickChoice(c,m,value("transfer_sound"),transferY,new String[]{"يوجد","لا يوجد"},new float[]{281,247});
        tickChoice(c,m,value("transfer_leaks"),transferY,new String[]{"يوجد","لا يوجد"},new float[]{213,177});
        tickChoice(c,m,value("shaft"),transferY,new String[]{"جيد","غير جيد"},new float[]{127.5f,59});
        drawWrappedRtl(c,pdfPaint(9f),value("transfer_notes"),510,503,475,12,4);

        // الدفرنس: صف الاسم/الاختيارات ثم صف أبيض مستقل لوضع ✓.
        drawDiffRow(c,m,618,"front_diff_state","front_diff_gears","front_diff_sound","front_diff_leaks","front_axles");
        drawDiffRow(c,m,652,"rear_diff_state","rear_diff_gears","rear_diff_sound","rear_diff_leaks","rear_axles");
        drawWrappedRtl(c,pdfPaint(9f),value("diff_notes"),510,666,475,12,4);

        // نظام التكييف والتبريد في نهاية الصفحة الثانية.
        String ac = value("ac_status");
        if ("يعمل".equals(ac)) tick(c,m,329.5f,722.5f);
        else if ("لا يعمل".equals(ac)) tick(c,m,215.5f,722.5f);
    }

    private void drawDiffRow(Canvas c, Paint m, float y, String state, String gears, String sound, String leaks, String axles) {
        tickChoice(c,m,value(state),y,new String[]{"جيد","غير جيد"},new float[]{435,392.5f});
        tickChoice(c,m,value(gears),y,new String[]{"جيد","غير جيد"},new float[]{353.5f,316.5f});
        tickChoice(c,m,value(sound),y,new String[]{"يوجد","لا يوجد"},new float[]{281,247});
        tickChoice(c,m,value(leaks),y,new String[]{"يوجد","لا يوجد"},new float[]{213,177});
        tickChoice(c,m,value(axles),y,new String[]{"جيد","غير جيد"},new float[]{127.5f,59});
    }

    private void overlayPage3(Canvas c, int w, int h) {
        Paint m = pdfPaint(9.5f);
        Paint t = pdfPaint(9f);

        String[] suspension = {"مجموعة الدركسون","الذراعات","المقصات","المساعدات","الكعكات","الفرامل","السبرنجه","عمود التوازن"};
        float y = 133f;
        for (String x : suspension) {
            String v = value("sus_"+x);
            if ("جيد".equals(v)) tick(c,m,443,y);
            else if ("غير جيد".equals(v)) tick(c,m,391,y);
            y += 24.45f;
        }
        // الملاحظات على السطر المنقط، وليس أسفل عنوان الملاحظات.
        drawWrappedRtl(c,t,value("suspension_notes"),558,367,505,13,2);

        // الإطارات: مركز مربع جيد/تالف بالضبط.
        tireTick(c,m,value("tire_fl"),89,145);
        tireTick(c,m,value("tire_fr"),187,145);
        tireTick(c,m,value("tire_rl"),89,292);
        tireTick(c,m,value("tire_rr"),187,292);

        drawWrappedRtl(c,pdfPaint(8.8f),value("road_test"),548,424,485,11.5f,1);

        // إشارات الطبلون: القالب يحتوي 18 رمزاً (3 أعمدة × 6 صفوف).
        // الخلايا البيضاء تقع يسار كل رمز مباشرة؛ نضع ✓ في مركز الخلية المقابلة فقط.
        String[] lights = {
                "4WD", "Brake", "صيانة/Record maint", "T-BELT", "مفتاح/Immobilizer", "تحذير الدركسون",
                "حرارة المحرك", "شمعات التسخين/Glow Plug", "ABS", "ضغط زيت المحرك", "حرارة سائل التبريد", "ESP",
                "Check Engine", "مستوى زيت المحرك", "مانع الانزلاق/Traction", "بطارية", "Airbag", "ضغط الإطارات"
        };
        Map<String,float[]> lightBoxes = new HashMap<>();
        // v12: exact centers of the 18 WHITE check cells in the FINAL dashboard template.
        // IMPORTANT: these are measured directly from page 3 (595×842 points).
        // There is no legacy/secondary dashboard drawing path and no calculated row spacing.
        final float[] dashboardY = new float[]{533.0f, 570.5f, 607.5f, 645.0f, 670.5f, 696.0f};
        // Left group: white cell x=240..283 -> center 261.5
        lightBoxes.put("4WD", new float[]{261.5f,dashboardY[0]});
        lightBoxes.put("Brake", new float[]{261.5f,dashboardY[1]});
        lightBoxes.put("صيانة/Record maint", new float[]{261.5f,dashboardY[2]});
        lightBoxes.put("T-BELT", new float[]{261.5f,dashboardY[3]});
        lightBoxes.put("مفتاح/Immobilizer", new float[]{261.5f,dashboardY[4]});
        lightBoxes.put("تحذير الدركسون", new float[]{261.5f,dashboardY[5]});
        // Middle group: white cell x=371..397 -> center 384.0
        lightBoxes.put("حرارة المحرك", new float[]{384.0f,dashboardY[0]});
        lightBoxes.put("شمعات التسخين/Glow Plug", new float[]{384.0f,dashboardY[1]});
        lightBoxes.put("ABS", new float[]{384.0f,dashboardY[2]});
        lightBoxes.put("ضغط زيت المحرك", new float[]{384.0f,dashboardY[3]});
        lightBoxes.put("حرارة سائل التبريد", new float[]{384.0f,dashboardY[4]});
        lightBoxes.put("ESP", new float[]{384.0f,dashboardY[5]});
        // Right group: white cell x=471..501 -> center 486.0
        lightBoxes.put("Check Engine", new float[]{486.0f,dashboardY[0]});
        lightBoxes.put("مستوى زيت المحرك", new float[]{486.0f,dashboardY[1]});
        lightBoxes.put("مانع الانزلاق/Traction", new float[]{486.0f,dashboardY[2]});
        lightBoxes.put("بطارية", new float[]{486.0f,dashboardY[3]});
        lightBoxes.put("Airbag", new float[]{486.0f,dashboardY[4]});
        lightBoxes.put("ضغط الإطارات", new float[]{486.0f,dashboardY[5]});
        for (String x : lights) {
            if ("توجد إشارة".equals(value("light_"+x))) {
                float[] q=lightBoxes.get(x);
                if(q!=null) tick(c,m,q[0],q[1]);
            }
        }
        drawCenteredAutoFit(c,value("inspector"),85,566,110,10.0f,6.0f);
    }


    private void addCarPhotosPage(PdfDocument outDoc, int pageW, int pageH, int pageNumber) {
        PdfDocument.Page page = outDoc.startPage(new PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create());
        Canvas c = page.getCanvas();
        c.drawColor(Color.WHITE);

        Paint title = pdfPaint(20f);
        title.setTypeface(Typeface.create("sans", Typeface.BOLD));
        title.setTextAlign(Paint.Align.CENTER);
        c.drawText("صور السيارة المرفقة بتقرير الفحص", pageW/2f, 55, title);

        Paint info = pdfPaint(10f);
        info.setTextAlign(Paint.Align.RIGHT);
        String date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date());
        c.drawText("مالك السيارة: " + value("owner"), pageW-35, 82, info);
        c.drawText("رقم اللوحة: " + value("plate") + "    التاريخ: " + date, pageW-35, 100, info);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.DKGRAY);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1.2f);

        float margin = 30f;
        float gap = 14f;
        float top = 125f;
        float bottom = pageH - 50f;
        float boxW = (pageW - 2*margin - gap) / 2f;
        float boxH = (bottom - top - gap) / 2f;
        RectF[] boxes = new RectF[]{
                new RectF(margin, top, margin+boxW, top+boxH),
                new RectF(margin+boxW+gap, top, pageW-margin, top+boxH),
                new RectF(margin, top+boxH+gap, margin+boxW, bottom),
                new RectF(margin+boxW+gap, top+boxH+gap, pageW-margin, bottom)
        };

        Paint label = pdfPaint(9f);
        label.setTextAlign(Paint.Align.CENTER);
        for (int i=0; i<4; i++) {
            c.drawRect(boxes[i], border);
            String path = carPhotoPaths[i];
            if (path != null && !path.isEmpty() && new File(path).exists()) {
                Bitmap photo = decodeScaledBitmap(path, 1800, 1400);
                if (photo != null) {
                    RectF inset = new RectF(boxes[i].left+5, boxes[i].top+5, boxes[i].right-5, boxes[i].bottom-5);
                    drawBitmapCenterCrop(c, photo, inset);
                    photo.recycle();
                }
            } else {
                c.drawText("الصورة " + (i+1) + " - غير مضافة", boxes[i].centerX(), boxes[i].centerY(), label);
            }
        }
        outDoc.finishPage(page);
    }

    private void tireTick(Canvas c, Paint m, String value, float cx, float y) {
        if ("جيد".equals(value)) tick(c,m,cx+22,y);
        else if ("تالف".equals(value)) tick(c,m,cx-12,y);
    }

    private void tickChoice(Canvas c, Paint m, String value, float y, String[] values, float[] xs) {
        for (int i=0;i<values.length && i<xs.length;i++) if (values[i].equals(value)) { tick(c,m,xs[i],y); return; }
    }

    private void tick(Canvas c, Paint p, float x, float y) {
        // Draw a compact check mark geometrically so x/y are the true center of the mark.
        // This avoids the font-baseline shift that previously pushed ✓ onto labels.
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setColor(Color.BLACK);
        q.setStyle(Paint.Style.STROKE);
        q.setStrokeWidth(1.35f);
        q.setStrokeCap(Paint.Cap.ROUND);
        q.setStrokeJoin(Paint.Join.ROUND);
        Path path = new Path();
        path.moveTo(x - 3.2f, y);
        path.lineTo(x - 0.8f, y + 2.6f);
        path.lineTo(x + 4.2f, y - 3.3f);
        c.drawPath(path, q);
    }

    private Paint pdfPaint(float size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(Color.BLACK); p.setTextSize(size); p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        return p;
    }

    private String cleanChoice(String s) { return (s==null || s.equals("اختر")) ? "" : s; }

    private void drawCenteredAutoFit(Canvas c, String text, float cx, float y, float maxWidth, float maxSize, float minSize) {
        if (text == null) text = "";
        Paint p = pdfPaint(maxSize);
        float size = maxSize;
        while (size > minSize && p.measureText(text) > maxWidth) {
            size -= 0.35f;
            p.setTextSize(size);
        }
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(text, cx, y, p);
    }

    private void drawCentered(Canvas c, Paint p, String text, float cx, float y, float maxWidth) {
        if (text == null) text = "";
        text = fitText(p,text,maxWidth);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(text,cx,y,p);
    }

    private void drawRtl(Canvas c, Paint p, String text, float right, float y, float maxWidth) {
        if (text == null || text.trim().isEmpty()) return;
        text = fitText(p,text,maxWidth);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(text,right,y,p);
    }

    private String fitText(Paint p, String text, float maxWidth) {
        if (p.measureText(text) <= maxWidth) return text;
        String ell = "…";
        int n = text.length();
        while (n>1 && p.measureText(text.substring(0,n)+ell)>maxWidth) n--;
        return text.substring(0,Math.max(1,n))+ell;
    }

    private void drawWrappedRtl(Canvas c, Paint p, String text, float right, float y, float maxWidth, float lineHeight, int maxLines) {
        if (text == null || text.trim().isEmpty()) return;
        String[] words = text.trim().split("\\s+");
        List<String> lines = new ArrayList<>();
        String cur = "";
        for (String word : words) {
            String test = cur.isEmpty()?word:cur+" "+word;
            if (p.measureText(test)<=maxWidth) cur=test;
            else { if(!cur.isEmpty()) lines.add(cur); cur=word; if(lines.size()>=maxLines) break; }
        }
        if (lines.size()<maxLines && !cur.isEmpty()) lines.add(cur);
        p.setTextAlign(Paint.Align.RIGHT);
        for (int i=0;i<lines.size() && i<maxLines;i++) c.drawText(lines.get(i), right, y+i*lineHeight, p);
    }

    private void drawBitmapCenterCrop(Canvas c, Bitmap b, RectF dst) {
        float srcRatio = b.getWidth()/(float)b.getHeight();
        float dstRatio = dst.width()/dst.height();
        Rect src;
        if (srcRatio > dstRatio) {
            int newW = (int)(b.getHeight()*dstRatio);
            int left = (b.getWidth()-newW)/2;
            src = new Rect(left,0,left+newW,b.getHeight());
        } else {
            int newH = (int)(b.getWidth()/dstRatio);
            int top = (b.getHeight()-newH)/2;
            src = new Rect(0,top,b.getWidth(),top+newH);
        }
        c.drawBitmap(b,src,dst,null);
    }

    private Bitmap decodeScaledBitmap(String path, int reqW, int reqH) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds=true; BitmapFactory.decodeFile(path,o);
            int sample=1; while(o.outWidth/sample>reqW*2 || o.outHeight/sample>reqH*2) sample*=2;
            o.inJustDecodeBounds=false; o.inSampleSize=sample; return BitmapFactory.decodeFile(path,o);
        } catch(Exception e) { return null; }
    }

    private String safeName(String s) {
        if (s == null) return "";
        return s.replaceAll("[^\\p{L}\\p{N}_-]", "_");
    }

    private void showError(String title, Exception e) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(e.toString()).setPositiveButton("حسناً",null).show();
    }
}
