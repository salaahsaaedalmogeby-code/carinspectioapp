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
        Paint p = pdfPaint(9);
        // Date
        drawCentered(c,p,new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date()), 505, 198, 70);
        // Owner and vehicle table
        drawRtl(c,p,value("owner"), 440, 241, 310);
        drawCentered(c,p,value("manufacturer"), 420, 267, 92);
        drawCentered(c,p,value("vehicle_type"), 330, 267, 92);
        drawCentered(c,p,value("model"), 220, 267, 92);
        drawCentered(c,p,value("color"), 105, 267, 80);
        drawCentered(c,p,value("vin"), 420, 291, 92);
        drawCentered(c,p,value("engine_type"), 330, 291, 92);
        drawCentered(c,p,value("plate"), 220, 291, 92);
        drawCentered(c,p,value("drive"), 105, 291, 80);

        // Engine photo replaces the approximate car illustration on page 1 when available.
        if (enginePhotoPath != null && !enginePhotoPath.isEmpty() && new File(enginePhotoPath).exists()) {
            Bitmap photo = decodeScaledBitmap(enginePhotoPath, 1600, 1200);
            if (photo != null) {
                RectF box = new RectF(208, 322, 553, 581);
                drawBitmapCenterCrop(c, photo, box);
                Paint border = new Paint(Paint.ANTI_ALIAS_FLAG); border.setStyle(Paint.Style.STROKE); border.setStrokeWidth(1.2f); border.setColor(Color.DKGRAY);
                c.drawRect(box, border);
                photo.recycle();
            }
        }

        // Add selected body/base values in their notes zones so the exact template remains intact.
        p = pdfPaint(8);
        drawRtl(c,p,"النتيجة: " + cleanChoice(value("base")) + "   |   " + value("body_notes"), 295, 630, 250);
        drawRtl(c,p,"النتيجة: " + cleanChoice(value("body")), 295, 682, 250);
        drawRtl(c,p,value("general_notes"), 520, 719, 450);

        // Interior states inside the large left table (compact symbols).
        String[] interior = {"الزجاجات","فتحة السقف","مقابض الأبواب","الإضاءة والأنوار","الإشارات (الاصطبات)","الديكورات","الشنطة الخلفية","الأبواب الخلفية","الشاشة أو المسجل","المرايات","المساحات","المقاعد","الطبلون"};
        float y = 385;
        Paint mark = pdfPaint(8);
        for (String x : interior) {
            String v = value("int_"+x);
            if ("سليم".equals(v)) drawCentered(c,mark,"✓",69,y,18);
            else if ("غير سليم".equals(v)) drawCentered(c,mark,"✓",110,y,18);
            y += 20.2f;
        }
    }

    private void overlayPage2(Canvas c, int w, int h) {
        Paint p = pdfPaint(7.5f);
        // Notes areas are intentionally used for readable summaries while preserving the original table design.
        String engine = "نوع: "+cleanChoice(value("engine_kind"))+"  | وضع: "+cleanChoice(value("engine_seal"))+"  | صرفية: "+cleanChoice(value("engine_consumption"))+"  | أصوات: "+cleanChoice(value("engine_sound"))+"  | تهريب: "+cleanChoice(value("engine_leaks"))+"  | أداء: "+cleanChoice(value("engine_perf"))+"  | حرارة: "+cleanChoice(value("engine_temp"))+"  | دخان: "+cleanChoice(value("engine_smoke"))+"  | رجفة: "+cleanChoice(value("engine_vibration"));
        drawWrappedRtl(c,p,engine,560,132,520,12,3);
        drawWrappedRtl(c,p,value("engine_notes"),560,170,520,12,3);

        String trans = "النوع: "+cleanChoice(value("trans_type"))+" | الاسبيت: "+cleanChoice(value("clutch"))+" | التعشيق: "+cleanChoice(value("shifting"))+" | أصوات: "+cleanChoice(value("trans_sound"))+" | تهريب: "+cleanChoice(value("trans_leaks"))+" | الأداء: "+cleanChoice(value("trans_perf"));
        drawWrappedRtl(c,p,trans,560,306,520,12,3);
        drawWrappedRtl(c,p,value("trans_notes"),560,340,520,12,2);

        String transfer = "النوع: "+cleanChoice(value("transfer_type"))+" | الحالة: "+cleanChoice(value("transfer_seal"))+" | التعشيق: "+cleanChoice(value("transfer_shift"))+" | أصوات: "+cleanChoice(value("transfer_sound"))+" | تهريب: "+cleanChoice(value("transfer_leaks"))+" | صرة الدوران: "+cleanChoice(value("shaft"));
        drawWrappedRtl(c,p,transfer,560,470,520,12,3);
        drawWrappedRtl(c,p,value("transfer_notes"),560,504,520,12,2);

        String diff = "أمامي: حالة "+cleanChoice(value("front_diff_state"))+"، كوارين "+cleanChoice(value("front_diff_gears"))+"، أصوات "+cleanChoice(value("front_diff_sound"))+"، تهريب "+cleanChoice(value("front_diff_leaks"))+"، عكوس "+cleanChoice(value("front_axles"))+"   | خلفي: حالة "+cleanChoice(value("rear_diff_state"))+"، كوارين "+cleanChoice(value("rear_diff_gears"))+"، أصوات "+cleanChoice(value("rear_diff_sound"))+"، تهريب "+cleanChoice(value("rear_diff_leaks"))+"، عكوس "+cleanChoice(value("rear_axles"));
        drawWrappedRtl(c,p,diff,560,680,520,12,4);
        drawWrappedRtl(c,p,value("diff_notes"),560,728,520,12,3);
    }

    private void overlayPage3(Canvas c, int w, int h) {
        Paint p = pdfPaint(7.5f);
        String[] suspension = {"مجموعة الدركسون","الذراعات","المقصات","المساعدات","الكعكات","الفرامل","السبرنجه","عمود التوازن","أخرى"};
        float y = 80;
        for (String x : suspension) {
            String v = value("sus_"+x);
            if (!"اختر".equals(v)) drawRtl(c,p,v,420,y,90);
            y += 25.3f;
        }
        drawWrappedRtl(c,p,value("suspension_notes"),560,330,520,12,3);

        // Tire condition near each tire position.
        Paint tp = pdfPaint(9);
        drawCentered(c,tp,cleanChoice(value("tire_fl")),135,113,70);
        drawCentered(c,tp,cleanChoice(value("tire_fr")),260,113,70);
        drawCentered(c,tp,cleanChoice(value("tire_rl")),135,250,70);
        drawCentered(c,tp,cleanChoice(value("tire_rr")),260,250,70);

        drawWrappedRtl(c,p,value("road_test"),560,390,520,13,4);

        String[] lights = {"Check Engine","زيت المحرك","حرارة المحرك","بطارية","ABS","Brake","ESP","T-BELT","4WD","Airbag","ضغط الإطارات","مفتاح/Immobilizer","صيانة/Record maint"};
        StringBuilder sb = new StringBuilder();
        for (String x : lights) {
            String v = value("light_"+x);
            if ("توجد إشارة".equals(v)) { if (sb.length()>0) sb.append("، "); sb.append(x); }
        }
        drawWrappedRtl(c,p,"الإشارات الموجودة: " + (sb.length()==0?"لا توجد إشارات مسجلة":sb.toString()),560,610,270,12,6);
        drawWrappedRtl(c,p,value("computer_notes"),560,690,270,12,4);
        drawRtl(c,p,value("inspector"),220,680,150);
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
