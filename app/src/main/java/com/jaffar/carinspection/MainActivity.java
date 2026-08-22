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
        addText("base_notes", "ملاحظات القاعدة");
        addText("body_notes", "ملاحظات البودي");

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
        addChoice("ac_status", "نظام التكييف والتبريد", new String[]{"اختر","يعمل","لا يعمل"});

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

    // v7: calibrated against the final user-supplied PDF template, cell by cell.
    private void overlayPage1(Canvas c, int w, int h) {
        Paint text = pdfPaint(10.5f);
        Paint mark = pdfPaint(10.5f);
        String date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date());
        drawCentered(c,text,date,500,191,70);
        drawRtl(c,pdfPaint(11f),value("owner"), 420, 233, 360);

        // بيانات السيارة: مراكز الخانات البيضاء الفعلية في القالب النهائي.
        drawCentered(c,text,value("manufacturer"), 423, 254, 66);
        drawCentered(c,text,value("vehicle_type"), 350, 254, 65);
        drawCentered(c,text,value("model"), 286, 254, 48);
        drawCentered(c,text,value("color"), 116, 254, 45);
        drawCentered(c,text,value("vin"), 423, 276, 66);
        drawCentered(c,text,value("engine_type"), 350, 276, 65);
        drawCentered(c,text,value("plate"), 286, 276, 48);
        drawCentered(c,text,value("drive"), 116, 276, 45);

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
        drawWrappedRtl(c,pdfPaint(9f),value("general_notes"),470,710,410,12,4);
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

        String[] suspension = {"مجموعة الدركسون","الذراعات","المقصات","المساعدات","الكعكات","الفرامل","السبرنجه","عمود التوازن","أخرى"};
        float y = 133f;
        for (String x : suspension) {
            String v = value("sus_"+x);
            if ("جيد".equals(v)) tick(c,m,443,y);
            else if ("غير جيد".equals(v)) tick(c,m,391,y);
            y += 24.45f;
        }
        // الملاحظات على السطر المنقط، وليس أسفل عنوان الملاحظات.
        drawWrappedRtl(c,t,value("suspension_notes"),470,340,185,12,2);

        // الإطارات: مركز مربع جيد/تالف بالضبط.
        tireTick(c,m,value("tire_fl"),89,145);
        tireTick(c,m,value("tire_fr"),187,145);
        tireTick(c,m,value("tire_rl"),89,292);
        tireTick(c,m,value("tire_rr"),187,292);

        drawWrappedRtl(c,pdfPaint(9f),value("road_test"),520,428,330,12,2);

        // مربعات إشارات الطبلون هي الخلايا البيضاء الملاصقة لكل أيقونة.
        String[] lights = {"Check Engine","زيت المحرك","حرارة المحرك","بطارية","ABS","Brake","ESP","T-BELT","4WD","Airbag","ضغط الإطارات","مفتاح/Immobilizer","صيانة/Record maint"};
        Map<String,float[]> lightBoxes = new HashMap<>();
        lightBoxes.put("Check Engine", new float[]{475,567.5f});
        lightBoxes.put("زيت المحرك", new float[]{475,592});
        lightBoxes.put("حرارة المحرك", new float[]{375,567.5f});
        lightBoxes.put("بطارية", new float[]{475,640.5f});
        lightBoxes.put("ABS", new float[]{375,616});
        lightBoxes.put("Brake", new float[]{277,592});
        lightBoxes.put("ESP", new float[]{375,689});
        lightBoxes.put("T-BELT", new float[]{277,640.5f});
        lightBoxes.put("4WD", new float[]{277,567.5f});
        lightBoxes.put("Airbag", new float[]{475,665});
        lightBoxes.put("ضغط الإطارات", new float[]{475,689});
        lightBoxes.put("مفتاح/Immobilizer", new float[]{375,592});
        lightBoxes.put("صيانة/Record maint", new float[]{277,616});
        for (String x : lights) {
            if ("توجد إشارة".equals(value("light_"+x))) {
                float[] q=lightBoxes.get(x); if(q!=null) tick(c,m,q[0],q[1]);
            }
        }
        drawWrappedRtl(c,t,value("computer_notes"),245,746,190,11,3);
        drawRtl(c,pdfPaint(10f),value("inspector"),145,566,110);
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
