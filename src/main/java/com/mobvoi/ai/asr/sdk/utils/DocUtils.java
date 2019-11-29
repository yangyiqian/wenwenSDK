package com.mobvoi.ai.asr.sdk.utils;

import java.io.FileOutputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

public class DocUtils {
    /**
     * Sample Code可以参考以下链接
     * http://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/xwpf/usermodel/examples/SimpleDocument.java
     */
    public static void toWord(String content, String outputFileName)   
         throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p1 = doc.createParagraph();
            XWPFRun r1 = p1.createRun();
            r1.setText(content);

            try (FileOutputStream out = new FileOutputStream(outputFileName)) {
                doc.write(out);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DocUtils.toWord("Hi hw r u?", "sample.docx");
    }   
  }   