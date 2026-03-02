package com.neuralseed;

/**
 * تعبير الوعي الكامل: فكرة + صورة + كلام (اختياري)
 */
public class Expression {
    
    public Thought thought;
    public MentalImage image;
    public String text; // قد يكون null إذا لم يرد الكلام
    
    public Expression(Thought t, MentalImage i, String txt) {
        this.thought = t;
        this.image = i;
        this.text = txt;
    }
    
    public boolean hasText() {
        return text != null && !text.isEmpty();
    }
}
