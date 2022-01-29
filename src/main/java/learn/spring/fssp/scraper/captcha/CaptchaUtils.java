package learn.spring.fssp.scraper.captcha;

import captchure.recognizer.CaptchaRecognizer;

import java.awt.image.BufferedImage;

public class CaptchaUtils {
    public static String recognizeCaptcha(BufferedImage captcha){
        try{
            return new CaptchaRecognizer().recognize(captcha);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
