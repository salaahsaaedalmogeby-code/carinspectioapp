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

public class MainActivity extends Activity {

    private static final int REQ_ENGINE_CAMERA = 1201;
    private LinearLayout form;
    private Button btnSave, btnPdf;
    private ImageView enginePhotoPreview;
    private TextView enginePhotoStatus;
    private Map<String, EditText> textFields = new LinkedHashMap<>();
    private Map<String, Spinner> choiceFields = new LinkedHashMap<>();
    private String currentInspectionId = null;
    private String enginePhotoPath = "";
    private String pendingPhotoPath = "";

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
        btnSave.setOnClickListener(v -> saveInspection(true));
        btnPdf.setOnClickListener(v -> generatePdf());
        newInspection();
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
        addText("body_notes", "ملاحظات الهيكل والبودي");

        form.addView(section("3) الأجزاء الداخلية للسيارة"));
        String[] interior = {"الزجاجات","فتحة السقف","مقابض الأبواب","الإضاءة والأنوار","الإشارات (الاصطبات)","الديكورات","الشنطة الخلفية","الأبواب الخلفية","الشاشة أو المسجل","المرايات","المساحات","المقاعد","الطبلون"};
        for (String x : interior) addChoice("int_"+x, x, intact);
        addText("interior_notes", "ملاحظات الأجزاء الداخلية");

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

        form.addView(section("8) التوجيه ونظام التعليق"));
        String[] suspension = {"مجموعة الدركسون","الذراعات","المقصات","المساعدات","الكعكات","الفرامل","السبرنجه","عمود التوازن","أخرى"};
        for (String x : suspension) addChoice("sus_"+x, x, goodBad);
        addText("suspension_notes", "ملاحظات التوجيه والتعليق");

        form.addView(section("9) حالة الإطارات"));
        addChoice("tire_fl", "الإطار الأمامي الأيسر", new String[]{"اختر","جيد","تالف"});
        addChoice("tire_fr", "الإطار الأمامي الأيمن", new String[]{"اختر","جيد","تالف"});
        addChoice("tire_rl", "الإطار الخلفي الأيسر", new String[]{"اختر","جيد","تالف"});
        addChoice("tire_rr", "الإطار الخلفي الأيمن", new String[]{"اختر","جيد","تالف"});

        form.addView(section("10) التجربة الميدانية للسيارة"));
        addText("road_test", "نتيجة التجربة الميدانية");

        form.addView(section("11) فحص الكمبيوتر وإشارات الطبلون"));
        String[] lights = {"Check Engine","زيت المحرك","حرارة المحرك","بطارية","ABS","Brake","ESP","T-BELT","4WD","Airbag","ضغط الإطارات","مفتاح/Immobilizer","صيانة/Record maint"};
        for (String x : lights) addChoice("light_"+x, x, new String[]{"اختر","لا توجد إشارة","توجد إشارة"});
        addText("computer_notes", "ملاحظات فحص الكمبيوتر");

        form.addView(section("12) بيانات التقرير"));
        addText("inspector", "اسم المهندس / الفاحص المختص");
        addText("general_notes", "ملاحظات عامة");
        refreshEnginePhotoPreview();
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
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
            refreshEnginePhotoPreview();
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

            for (int i=0; i<renderer.getPageCount(); i++) {
                PdfRenderer.Page rp = renderer.openPage(i);
                int pageW = rp.getWidth();
                int pageH = rp.getHeight();
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

    private void overlayPage1(Canvas c, int w, int h) {
        Paint text = pdfPaint(8.5f);
        Paint mark = pdfPaint(10.5f);
        String date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date());
        drawCentered(c,text,date,505,200,70);
        drawRtl(c,text,value("owner"), 550, 242, 360);

        // بيانات السيارة: كل قيمة داخل المستطيل الأبيض المخصص لها.
        drawCentered(c,text,value("manufacturer"), 420, 272, 72);
        drawCentered(c,text,value("vehicle_type"), 330, 272, 70);
        drawCentered(c,text,value("model"), 220, 272, 70);
        drawCentered(c,text,value("color"), 105, 272, 55);
        drawCentered(c,text,value("vin"), 420, 291, 72);
        drawCentered(c,text,value("engine_type"), 330, 291, 70);
        drawCentered(c,text,value("plate"), 220, 291, 70);
        drawCentered(c,text,value("drive"), 105, 291, 55);

        if (enginePhotoPath != null && !enginePhotoPath.isEmpty() && new File(enginePhotoPath).exists()) {
            Bitmap photo = decodeScaledBitmap(enginePhotoPath, 1600, 1200);
            if (photo != null) {
                RectF box = new RectF(198, 322, 568, 584);
                drawBitmapCenterCrop(c, photo, box);
                photo.recycle();
            }
        }

        // الأجزاء الداخلية: صح داخل جيد أو سيئ.
        String[] interior = {"الزجاجات","فتحة السقف","مقابض الأبواب","الإضاءة والأنوار","الإشارات (الاصطبات)","الديكورات","الشنطة الخلفية","الأبواب الخلفية","الشاشة أو المسجل","المرايات","المساحات","المقاعد","الطبلون"};
        float y = 363;
        for (String x : interior) {
            String v = value("int_"+x);
            if ("سليم".equals(v)) tick(c,mark,92,y);
            else if ("غير سليم".equals(v)) tick(c,mark,51,y);
            y += 16.6f;
        }

        // القاعدة: المربعات من اليمين إلى اليسار.
        String base = value("base");
        if ("سليم".equals(base)) tick(c,mark,477,615);
        else if ("رش كبير".equals(base)) tick(c,mark,443,615);
        else if ("رش خفيف".equals(base)) tick(c,mark,409,615);
        else if ("سمكرة كبير".equals(base)) tick(c,mark,373,615);
        else if ("سمكرة خفيف".equals(base)) tick(c,mark,334,615);
        drawWrappedRtl(c,pdfPaint(7.5f),value("body_notes"),294,613,250,11,2);

        String body = value("body");
        if ("سليم".equals(body)) tick(c,mark,477,667);
        else if ("مقلوب كامل".equals(body)) tick(c,mark,443,667);
        else if ("قلبة جانبية".equals(body)) tick(c,mark,409,667);
        else if ("مرشوش كامل".equals(body)) tick(c,mark,373,667);
        else if ("رشة جزئية (تلقيطات)".equals(body)) tick(c,mark,334,667);

        drawWrappedRtl(c,pdfPaint(8),value("general_notes"),520,724,440,12,4);
    }

    private void overlayPage2(Canvas c, int w, int h) {
        Paint m = pdfPaint(9.5f);
        Paint t = pdfPaint(7.2f);

        // فحص المحرك - العلامة داخل مربع الاختيار الصحيح.
        drawCentered(c,t,value("engine_kind"),516,112,52);
        tickChoice(c,m,value("engine_seal"),112,new String[]{"مختوم","مفكوك"},new float[]{467,443});
        tickChoice(c,m,value("engine_consumption"),112,new String[]{"يوجد","لا يوجد"},new float[]{413,389});
        tickChoice(c,m,value("engine_sound"),112,new String[]{"يوجد","لا يوجد"},new float[]{365,341});
        tickChoice(c,m,value("engine_leaks"),112,new String[]{"يوجد","لا يوجد"},new float[]{317,293});
        tickChoice(c,m,value("engine_perf"),112,new String[]{"ممتاز","جيد","متوسط","ضعيف"},new float[]{270,247,224,201});
        tickChoice(c,m,value("engine_temp"),112,new String[]{"طبيعي","توجد حرارة"},new float[]{177,153});
        tickChoice(c,m,value("engine_smoke"),112,new String[]{"يوجد","لا يوجد"},new float[]{129,105});
        tickChoice(c,m,value("engine_vibration"),112,new String[]{"يوجد","لا يوجد"},new float[]{69,45});
        drawWrappedRtl(c,t,value("engine_notes"),570,148,535,11,6);

        // ناقل الحركة.
        tickChoice(c,m,value("trans_type"),321,new String[]{"أوتوماتيك","عادي"},new float[]{515,486});
        tickChoice(c,m,value("clutch"),321,new String[]{"مختوم","مفكوك"},new float[]{443,416});
        tickChoice(c,m,value("shifting"),321,new String[]{"جيد","غير جيد"},new float[]{374,344});
        tickChoice(c,m,value("trans_sound"),321,new String[]{"يوجد","لا يوجد"},new float[]{303,276});
        tickChoice(c,m,value("trans_leaks"),321,new String[]{"يوجد","لا يوجد"},new float[]{241,214});
        tickChoice(c,m,value("trans_perf"),321,new String[]{"ممتاز","جيد","متوسط","ضعيف"},new float[]{177,144,111,78});
        drawWrappedRtl(c,t,value("trans_notes"),570,354,535,11,4);

        // الدبل.
        tickChoice(c,m,value("transfer_type"),479,new String[]{"ذاتي","يدوي"},new float[]{515,488});
        tickChoice(c,m,value("transfer_seal"),479,new String[]{"مختوم","مفكوك"},new float[]{445,414});
        tickChoice(c,m,value("transfer_shift"),479,new String[]{"جيد","غير جيد"},new float[]{376,345});
        tickChoice(c,m,value("transfer_sound"),479,new String[]{"يوجد","لا يوجد"},new float[]{306,276});
        tickChoice(c,m,value("transfer_leaks"),479,new String[]{"يوجد","لا يوجد"},new float[]{240,213});
        tickChoice(c,m,value("shaft"),479,new String[]{"جيد","غير جيد"},new float[]{143,79});
        drawWrappedRtl(c,t,value("transfer_notes"),570,512,535,11,3);

        // الدفرنس الأمامي والخلفي.
        drawDiffRow(c,m,589,"front_diff_state","front_diff_gears","front_diff_sound","front_diff_leaks","front_axles");
        drawDiffRow(c,m,625,"rear_diff_state","rear_diff_gears","rear_diff_sound","rear_diff_leaks","rear_axles");
        drawWrappedRtl(c,t,value("diff_notes"),570,681,535,11,7);
    }

    private void drawDiffRow(Canvas c, Paint m, float y, String state, String gears, String sound, String leaks, String axles) {
        tickChoice(c,m,value(state),y,new String[]{"جيد","غير جيد"},new float[]{438,405});
        tickChoice(c,m,value(gears),y,new String[]{"جيد","غير جيد"},new float[]{368,336});
        tickChoice(c,m,value(sound),y,new String[]{"يوجد","لا يوجد"},new float[]{300,269});
        tickChoice(c,m,value(leaks),y,new String[]{"يوجد","لا يوجد"},new float[]{230,198});
        tickChoice(c,m,value(axles),y,new String[]{"جيد","غير جيد"},new float[]{132,65});
    }

    private void overlayPage3(Canvas c, int w, int h) {
        Paint m = pdfPaint(9.5f);
        Paint t = pdfPaint(7.5f);

        // التوجيه والتعليق: جيد/غير جيد داخل العمود الصحيح.
        String[] suspension = {"مجموعة الدركسون","الذراعات","المقصات","المساعدات","الكعكات","الفرامل","السبرنجه","عمود التوازن","أخرى"};
        float y = 129;
        for (String x : suspension) {
            String v = value("sus_"+x);
            if ("جيد".equals(v)) tick(c,m,444,y);
            else if ("غير جيد".equals(v)) tick(c,m,394,y);
            y += 24.7f;
        }
        drawWrappedRtl(c,t,value("suspension_notes"),360,350,75,11,3);

        // الإطارات: علامة صح داخل جيد أو تالف لكل إطار.
        tireTick(c,m,value("tire_fl"),89,145);
        tireTick(c,m,value("tire_fr"),187,145);
        tireTick(c,m,value("tire_rl"),89,292);
        tireTick(c,m,value("tire_rr"),187,292);

        drawWrappedRtl(c,pdfPaint(8),value("road_test"),555,431,360,13,3);

        // إشارات الطبلون: كل إشارة لها مربع أبيض ملاصق للصورة؛ نضع ✓ فيه عند وجود الإشارة.
        String[] lights = {"Check Engine","زيت المحرك","حرارة المحرك","بطارية","ABS","Brake","ESP","T-BELT","4WD","Airbag","ضغط الإطارات","مفتاح/Immobilizer","صيانة/Record maint"};
        Map<String,float[]> lightBoxes = new HashMap<>();
        lightBoxes.put("Check Engine", new float[]{506,574});
        lightBoxes.put("زيت المحرك", new float[]{506,622});
        lightBoxes.put("حرارة المحرك", new float[]{403,574});
        lightBoxes.put("بطارية", new float[]{506,694});
        lightBoxes.put("ABS", new float[]{403,646});
        lightBoxes.put("Brake", new float[]{300,598});
        lightBoxes.put("ESP", new float[]{403,718});
        lightBoxes.put("T-BELT", new float[]{300,670});
        lightBoxes.put("4WD", new float[]{300,574});
        lightBoxes.put("Airbag", new float[]{506,718});
        lightBoxes.put("ضغط الإطارات", new float[]{506,742});
        lightBoxes.put("مفتاح/Immobilizer", new float[]{403,598});
        lightBoxes.put("صيانة/Record maint", new float[]{300,646});
        for (String x : lights) {
            if ("توجد إشارة".equals(value("light_"+x))) {
                float[] q=lightBoxes.get(x); if(q!=null) tick(c,m,q[0],q[1]);
            }
        }
        drawWrappedRtl(c,t,value("computer_notes"),555,766,290,11,3);
        drawRtl(c,pdfPaint(8),value("inspector"),220,685,120);
    }

    private void tireTick(Canvas c, Paint m, String value, float cx, float y) {
        if ("جيد".equals(value)) tick(c,m,cx+22,y);
        else if ("تالف".equals(value)) tick(c,m,cx-12,y);
    }

    private void tickChoice(Canvas c, Paint m, String value, float y, String[] values, float[] xs) {
        for (int i=0;i<values.length && i<xs.length;i++) if (values[i].equals(value)) { tick(c,m,xs[i],y); return; }
    }

    private void tick(Canvas c, Paint p, float x, float y) {
        drawCentered(c,p,"✓",x,y,18);
    }

    private Paint pdfPaint(float size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(Color.BLACK); p.setTextSize(size); p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        return p;
    }

    private String cleanChoice(String s) { return (s==null || s.equals("اختر")) ? "" : s; }

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
