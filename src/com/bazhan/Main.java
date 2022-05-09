package com.bazhan;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws IOException,InterruptedException{
        new Main().run(new URL("https://github.com/Regitens"));
    }

    private static final Pattern IMG_PATTERN=Pattern.compile("[<]\\s*[iI][mM][gG]"+"\\s*[^>]*[sS][rR][cC]"+
            "\\s*[=]\\s*['\"]"+"([^'\"]*)['\"][^>]*[>]");
    private ExecutorService executor= Executors.newCachedThreadPool();
    private URL urlToProcess;

    //получает текст с веб страницы как только становится доступен
    public CompletableFuture<String> readPage(URL url){
        return CompletableFuture.supplyAsync(()->
        {
            String contents= null;
            try {
                contents = new String(url.openStream().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Read page from"+url);
                return contents;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        },executor);
    }

    //получает URL на веб странице
    public List<URL> getImageURLs (String webPage){
        try {
            var result=new ArrayList<URL>();
            Matcher matcher=IMG_PATTERN.matcher(webPage);
            while (matcher.find()){
                var url=new URL(urlToProcess, matcher.group(1));
                result.add(url);
            }
            System.out.println("Found URLs: "+result);
            return result;
        }
        catch (IOException e){
            throw new UncheckedIOException(e);
        }
    }

    public CompletableFuture<List<BufferedImage>> getImages (List<URL> urls){
        //выполнить задачу асинхронно и получить в итоге объект типа CompletableFuture
        return CompletableFuture.supplyAsync(()->
        {
            try{
                var result=new ArrayList<BufferedImage>();
                for (URL url:urls) {
                    result.add(ImageIO.read(url));
                    System.out.println("Loaded "+url);
                }
                return result;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, executor);
    }

    public void saveImages(List<BufferedImage> images)
    {
        System.out.println("Saving " + images.size() + " images");
        try
        {
            for (int i = 0; i < images.size(); i++)
            {
                String filename = "/tmp/image" + (i + 1) + ".png";
                ImageIO.write(images.get(i), "PNG", new File(filename));
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        executor.shutdown();
    }

    public void run(URL url) throws IOException, InterruptedException
    {
        urlToProcess = url;
        CompletableFuture.completedFuture(url)
                //Завершаемое будущее действие с URL в качестве аргумента
                // и составл будущего действия по сслыке на метод
                .thenComposeAsync(this::readPage, executor)
                .thenApply(this::getImageURLs)
                .thenCompose(this::getImages)
                .thenAccept(this::saveImages);
    }
}
