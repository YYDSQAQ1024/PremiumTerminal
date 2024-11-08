package me.wang.premiumterminal.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Password {
    private static final String lowStr = "abcdefghijklmnopqrstuvwxyz";
    private static final String specialStr = "@#$%^&*";
    private static final String numStr = "0123456789";

    private String password;

    // 随机获取字符串字符
    private static char getRandomChar(String str) {
        SecureRandom random = new SecureRandom();
        return str.charAt(random.nextInt(str.length()));
    }

    // 随机获取小写字符
    private static char getLowChar() {
        return getRandomChar(lowStr);
    }

    // 随机获取大写字符
    private static char getUpperChar() {
        return Character.toUpperCase(getLowChar());
    }

    // 随机获取数字字符
    private static char getNumChar() {
        return getRandomChar(numStr);
    }

    // 随机获取特殊字符
    private static char getSpecialChar() {
        return getRandomChar(specialStr);
    }

    //指定调用字符函数
    private static char getRandomChar(int funNum) {
        switch (funNum) {
            case 0:
                return getLowChar();
            case 1:
                return getUpperChar();
            case 2:
                return getNumChar();
            default:
                return getSpecialChar();
        }
    }

    public static String getRandomPwd(int num) {

        List<Character> list = new ArrayList<>(num);
        list.add(getLowChar());
        list.add(getUpperChar());
        list.add(getNumChar());
        list.add(getSpecialChar());

        for (int i = 4; i < num; i++) {
            SecureRandom random = new SecureRandom();
            int funNum = random.nextInt(4);
            list.add(getRandomChar(funNum));
        }

        Collections.shuffle(list);   // 打乱排序
        StringBuilder stringBuilder = new StringBuilder(list.size());
        for (Character c : list) {
            stringBuilder.append(c);
        }

        return stringBuilder.toString();
    }

    public Password(){
        this.password = getRandomPwd(8);
    }

    public void Respawn(){
        this.password = getRandomPwd(8);
    }

    public Boolean setPassword(String password) {
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&*]).{8,}$";
        Pattern pattern = Pattern.compile(passwordPattern);
        Matcher matcher = pattern.matcher(password);
        if (!matcher.matches()){
            return false;
        }
        this.password = password;
        return true;
    }

    @Override
    public String toString() {
        return this.password;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof  Password)){
            return false;
        }

        return super.equals(obj);
    }
}
