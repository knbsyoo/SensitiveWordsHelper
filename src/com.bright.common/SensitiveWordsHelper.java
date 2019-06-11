package sensitiveWords;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created By JianBin.Liu on 2019/6/5
 * Description: 敏感词帮助类
 */
public class SensitiveWordsHelper {

    /**
     * 文件存储路径
     */
    private final static String FILE_PATH = ".../sensitiveData.txt";

    /**
     * 敏感词库转化后的MAP
     */
    private static HashMap sensitiveWordMap;

    /**
     *  敏感词库
     */
    private static HashSet<String> sensitiveWords;

    /**
     * 匹配规则：2-最大限度匹配
     */
    private static final Integer MATCH_TYPE_MAX = 2;

    /**
     * 匹配规则：1-最小限度匹配
     */
    private static final Integer MATCH_TYPE_MIN = 1;


    private static SensitiveWordsHelper ourInstance = new SensitiveWordsHelper();

    public static SensitiveWordsHelper getInstance() {
        return ourInstance;
    }

    private SensitiveWordsHelper() {
    }

    /**
     * 数据初始化
     *
     * @throws IOException
     */
    public static void init() throws IOException {
        readFile();
        convert2HashMap();
    }

    /**
     * 读取敏感词文件
     */
    private static void readFile() throws IOException {
        sensitiveWords = new HashSet<String>();
        try {
            long start = new Date().getTime();
            URL url = new URL(FILE_PATH);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(3000);//连接超时时间设置为3s
            connection.setReadTimeout(20000);//读取超时时间设置为10s，敏感词文件有10M
            connection.setDoInput(true);//the application intends to read data from the URL connection.
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));String str;
            while (null != (str = reader.readLine())){
                sensitiveWords.add(str);
            }
            long end = new Date().getTime();
            System.out.println("读取文件到本地，并且转化为map集合，耗时：" + (end - start) + "毫秒");
            System.out.println(sensitiveWords.size());
        } catch (MalformedURLException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * 根据DFA算法，转化成特定的结构
     */
    public static void convert2HashMap(){
        long start = new Date().getTime();
        sensitiveWordMap = new HashMap();
        for(String word: sensitiveWords) {
            HashMap currentMap = sensitiveWordMap;//当前map指向sensitiveWordMap
            for (int i = 0; i < word.length(); i++) {
                String key = String.valueOf(word.charAt(i));
                if (!currentMap.containsKey(key)) {
                    HashMap wordMap = new HashMap<>();
                    wordMap.put("isEnd", false);//默认不是最后一个字符
                    currentMap.put(key, wordMap);
                    currentMap = wordMap;//指向下一级Map
                } else {
                    currentMap = (HashMap) currentMap.get(key);
                }
                if( i == word.length() - 1) currentMap.put("isEnd", true);
            }
        }
        long end = new Date().getTime();
        System.out.println("转化成DFA算法的数据结构，耗时：" + (end - start) + "毫秒");
    }

    /**
     *
     * 查询文本中敏感词出行的次数
     *
     * @param txt
     * @param type
     * @return
     */
    private static HashSet<String> findAllInTxt(String txt, Integer type){
        HashSet<String> result = new HashSet<>();
        if(null == sensitiveWordMap){
            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(null == txt || txt.length() == 0) return result;
        for(int i = 0; i < txt.length(); i++){
            String word = "";
            String key = String.valueOf(txt.charAt(i));
            word  += key;
            if(!sensitiveWordMap.containsKey(key)){//当前字符不是敏感词，跳过，判断下一个字符
                continue;
            }
            HashMap firstMap = (HashMap)sensitiveWordMap.get(key);
            if((boolean)(firstMap.get("isEnd"))){//当前字符是否为敏感词
                result.add(word);
                if(MATCH_TYPE_MIN == type) return result;//最小匹配限度规则：查找到第一个敏感词就返回
            }
            if(i == txt.length() - 1) break;//最后一个字符，不包含子集
            HashMap currentMap = (HashMap)sensitiveWordMap.get(key);//当前字符不是最后一个字符，依次判断字符串是否敏感词。
            for(int j = i + 1; j < txt.length(); j ++){
                String key2 = String.valueOf(txt.charAt(j));
                word += key2;
                if(currentMap.containsKey(key2)){
                    if((boolean)((HashMap)currentMap.get(key2)).get("isEnd")){
                        result.add(word);
                        if(MATCH_TYPE_MIN == type) return result;
                    }
                    currentMap = (HashMap)currentMap.get(key2);//指向下一个子集
                }else {
                    break;
                }

            }
        }
        return result;
    }

    /**
     * 判断文本中是否包含敏感词
     *
     * @param txt
     * @return
     */
    public static boolean check(String txt){
        HashSet words = findAllInTxt(txt, MATCH_TYPE_MIN);
        if(words.size() == 0) return false;
        return true;
    }

    /**
     * 查询文本中所有的敏感词
     *
     * @param txt
     * @return
     */
    public static HashSet<String> findAllInTxt(String txt) {
        return findAllInTxt(txt, MATCH_TYPE_MAX);
    }

    public static void main(String[] args) throws IOException {
        init();
        String text = "阿宾新华社北京6月5日电 国家主席习近平5日乘专机离开北京，应俄罗斯联邦总统普京邀请，对俄罗斯进行国事访问并出席第二十三届圣彼得堡国际经济论坛。\n" +
                "\n" +
                "　　陪同习近平出访的有：中共中央政治局委员、中央书记处书记、中央办公厅主任丁薛祥，中共中央政治局委员、八九政治中央外事工作委员会办公室主任杨洁篪，国务委员兼东北独立外交部长王毅，全国辦毕业政协副主席、惩公安国家发展和改革委员会主任何立峰等。";
        long start = new Date().getTime();
        boolean isContainSW = check(text);
        long end = new Date().getTime();
        System.out.println("isContainSW:" + isContainSW);
        System.out.println("分析该字符串，耗时：" + (end - start) + "毫秒");

        long start2 = new Date().getTime();
        HashSet<String> sets = findAllInTxt(text);
        long end2 = new Date().getTime();
        System.out.println("sets:" + sets.toString());
        System.out.println("分析该字符串，耗时：" + (end2 - start2) + "毫秒");
    }
}
