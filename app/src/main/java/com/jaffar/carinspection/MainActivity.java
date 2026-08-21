package com.jaffar.carinspection;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout form;
    Button btnSave, btnPdf;
    Map<String, EditText> textFields = new LinkedHashMap<>();
    Map<String, Spinner> choiceFields = new LinkedHashMap<>();
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
        setContentView(com.jaffar.carinspection.R.layout.activity_main);

        form = findViewById(R.id.formContainer);
        btnSave = findViewById(R.id.btnSave);
        btnPdf = findViewById(R.id.btnPdf);

        findViewById(R.id.btnNew).setOnClickListener(v -> buildForm(false));
        findViewById(R.id.btnSaved).setOnClickListener(v -> buildForm(true));
        btnSave.setOnClickListener(v -> saveInspection());
        btnPdf.setOnClickListener(v -> generatePdf());

        buildForm(false);
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
        e.setSingleLine(!hint.contains("ملاحظ"));
        if (hint.contains("ملاحظ")) e.setMinLines(3);
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

    private void buildForm(boolean loadSaved) {
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

        if (loadSaved) loadInspection();
    }

    private void saveInspection() {
        SharedPreferences.Editor ed = getSharedPreferences("inspection", MODE_PRIVATE).edit();
        for (Map.Entry<String, EditText> e : textFields.entrySet())
            ed.putString("t_"+e.getKey(), e.getValue().getText().toString());
        for (Map.Entry<String, Spinner> e : choiceFields.entrySet())
            ed.putString("s_"+e.getKey(), e.getValue().getSelectedItem().toString());
        ed.putString("saved_at", new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date()));
        ed.apply();
        Toast.makeText(this, "تم حفظ الفحص", Toast.LENGTH_SHORT).show();
    }

    private void loadInspection() {
        SharedPreferences sp = getSharedPreferences("inspection", MODE_PRIVATE);
        for (Map.Entry<String, EditText> e : textFields.entrySet())
            e.getValue().setText(sp.getString("t_"+e.getKey(), ""));
        for (Map.Entry<String, Spinner> e : choiceFields.entrySet()) {
            String val = sp.getString("s_"+e.getKey(), "اختر");
            Spinner s = e.getValue();
            for (int i=0; i<s.getCount(); i++) {
                if (s.getItemAtPosition(i).toString().equals(val)) { s.setSelection(i); break; }
            }
        }
        Toast.makeText(this, "تم تحميل آخر فحص محفوظ", Toast.LENGTH_SHORT).show();
    }

    private String value(String key) {
        if (textFields.containsKey(key)) return textFields.get(key).getText().toString();
        if (choiceFields.containsKey(key)) return choiceFields.get(key).getSelectedItem().toString();
        return "";
    }

    private void drawHeader(Canvas c, Paint p, String pageTitle, int page) {
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(20);
        c.drawText("مركز الجنرال أوتوكار", 298, 42, p);
        p.setTextSize(16);
        c.drawText("تقرير الفحص الفني وتقييم أضرار السيارة", 298, 68, p);
        p.setTextSize(13);
        c.drawText(pageTitle, 298, 94, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(9);
        c.drawText("صفحة " + page, 30, 820, p);
        c.drawLine(25, 105, 570, 105, p);
    }

    private float drawKV(Canvas c, Paint p, float y, String label, String val) {
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(10);
        c.drawText(label + ":", 555, y, p);
        p.setTypeface(Typeface.DEFAULT);
        c.drawText(val == null ? "" : val, 395, y, p);
        return y + 17;
    }

    private void generatePdf() {
        try {
            saveInspection();
            PdfDocument doc = new PdfDocument();
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.BLACK);

            // Page 1
            PdfDocument.Page page1 = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
            Canvas c = page1.getCanvas();
            drawHeader(c,p,"بيانات السيارة وفحص الهيكل والأجزاء الداخلية",1);
            float y = 125;
            y=drawKV(c,p,y,"اسم مالك السيارة",value("owner"));
            y=drawKV(c,p,y,"الشركة المصنعة",value("manufacturer"));
            y=drawKV(c,p,y,"نوع المركبة",value("vehicle_type"));
            y=drawKV(c,p,y,"الموديل",value("model"));
            y=drawKV(c,p,y,"اللون",value("color"));
            y=drawKV(c,p,y,"رقم الهيكل",value("vin"));
            y=drawKV(c,p,y,"نوع المحرك",value("engine_type"));
            y=drawKV(c,p,y,"رقم اللوحة",value("plate"));
            y=drawKV(c,p,y,"الدفع",value("drive"));
            y+=12;
            y=drawKV(c,p,y,"القاعدة",value("base"));
            y=drawKV(c,p,y,"البودي",value("body"));
            y=drawKV(c,p,y,"ملاحظات الهيكل والبودي",value("body_notes"));
            String[] interior = {"الزجاجات","فتحة السقف","مقابض الأبواب","الإضاءة والأنوار","الإشارات (الاصطبات)","الديكورات","الشنطة الخلفية","الأبواب الخلفية","الشاشة أو المسجل","المرايات","المساحات","المقاعد","الطبلون"};
            for (String x : interior) y=drawKV(c,p,y,x,value("int_"+x));
            y=drawKV(c,p,y,"ملاحظات الداخلية",value("interior_notes"));
            doc.finishPage(page1);

            // Page 2
            PdfDocument.Page page2 = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, 2).create());
            c = page2.getCanvas();
            drawHeader(c,p,"فحص المحرك وناقل الحركة والدبل والدفرنس",2);
            y=125;
            String[][] page2Fields = {
                {"نوع المحرك","engine_kind"},{"وضع المحرك","engine_seal"},{"صرفية المحرك","engine_consumption"},
                {"أصوات في المحرك","engine_sound"},{"تهريبات في المحرك","engine_leaks"},{"أداء المحرك","engine_perf"},
                {"حرارة المحرك","engine_temp"},{"دخان أسود","engine_smoke"},{"رجفة في المحرك","engine_vibration"},
                {"ملاحظات المحرك","engine_notes"},
                {"نوع ناقل الحركة","trans_type"},{"حالة الاسبيت","clutch"},{"وضع التعشيق","shifting"},{"أصوات في الاسبيت","trans_sound"},
                {"تهريبات في الاسبيت","trans_leaks"},{"أداء ناقل الحركة","trans_perf"},{"ملاحظات ناقل الحركة","trans_notes"},
                {"نوع الدبل","transfer_type"},{"حالة الدبل","transfer_seal"},{"تعشيق الدبل","transfer_shift"},{"أصوات في الدبل","transfer_sound"},
                {"تهريبات في الدبل","transfer_leaks"},{"صرة الدوران","shaft"},{"ملاحظات الدبل","transfer_notes"},
                {"الدفرنس الأمامي - الحالة","front_diff_state"},{"الدفرنس الأمامي - الكوارين","front_diff_gears"},
                {"الدفرنس الأمامي - الأصوات","front_diff_sound"},{"الدفرنس الأمامي - التهريبات","front_diff_leaks"},{"الدفرنس الأمامي - العكوس","front_axles"},
                {"الدفرنس الخلفي - الحالة","rear_diff_state"},{"الدفرنس الخلفي - الكوارين","rear_diff_gears"},
                {"الدفرنس الخلفي - الأصوات","rear_diff_sound"},{"الدفرنس الخلفي - التهريبات","rear_diff_leaks"},{"الدفرنس الخلفي - العكوس","rear_axles"},
                {"ملاحظات الدفرنس","diff_notes"}
            };
            for (String[] f: page2Fields) y=drawKV(c,p,y,f[0],value(f[1]));
            doc.finishPage(page2);

            // Page 3
            PdfDocument.Page page3 = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, 3).create());
            c = page3.getCanvas();
            drawHeader(c,p,"التوجيه والتعليق والإطارات والتجربة وفحص الكمبيوتر",3);
            y=125;
            String[] suspension = {"مجموعة الدركسون","الذراعات","المقصات","المساعدات","الكعكات","الفرامل","السبرنجه","عمود التوازن","أخرى"};
            for (String x : suspension) y=drawKV(c,p,y,x,value("sus_"+x));
            y=drawKV(c,p,y,"ملاحظات التوجيه والتعليق",value("suspension_notes"));
            y+=8;
            y=drawKV(c,p,y,"الإطار الأمامي الأيسر",value("tire_fl"));
            y=drawKV(c,p,y,"الإطار الأمامي الأيمن",value("tire_fr"));
            y=drawKV(c,p,y,"الإطار الخلفي الأيسر",value("tire_rl"));
            y=drawKV(c,p,y,"الإطار الخلفي الأيمن",value("tire_rr"));
            y+=8;
            y=drawKV(c,p,y,"التجربة الميدانية",value("road_test"));
            String[] lights = {"Check Engine","زيت المحرك","حرارة المحرك","بطارية","ABS","Brake","ESP","T-BELT","4WD","Airbag","ضغط الإطارات","مفتاح/Immobilizer","صيانة/Record maint"};
            for (String x : lights) y=drawKV(c,p,y,x,value("light_"+x));
            y=drawKV(c,p,y,"ملاحظات الكمبيوتر",value("computer_notes"));
            y=drawKV(c,p,y,"الفاحص المختص",value("inspector"));
            y=drawKV(c,p,y,"ملاحظات عامة",value("general_notes"));
            doc.finishPage(page3);

            File dir = new File(getExternalFilesDir(null), "Reports");
            if (!dir.exists()) dir.mkdirs();
            String plate = value("plate").replaceAll("[^\\p{L}\\p{N}_-]", "_");
            if (plate.isEmpty()) plate = "car";
            String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File file = new File(dir, "Inspection_"+plate+"_"+time+".pdf");
            FileOutputStream out = new FileOutputStream(file);
            doc.writeTo(out);
            out.close();
            doc.close();

            new AlertDialog.Builder(this)
                .setTitle("تم إنشاء التقرير")
                .setMessage("تم حفظ ملف PDF في:\n" + file.getAbsolutePath())
                .setPositiveButton("حسناً", null)
                .show();

        } catch (Exception e) {
            new AlertDialog.Builder(this)
                .setTitle("تعذر إنشاء التقرير")
                .setMessage(e.toString())
                .setPositiveButton("حسناً", null)
                .show();
        }
    }
}
