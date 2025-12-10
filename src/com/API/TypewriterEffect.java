package API;

public class TypewriterEffect {

    public static void printWord(String text,int delay){
        for(int i=0;i<text.length();i++){
            System.out.print(text.charAt(i));

            try{
                Thread.sleep(delay);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println("打印被中断");
                return;
            }
        }
        System.out.println();
    }

    public static void main(String[] args){
        String message="Hello,World! 这是一个Java实现逐字打印效果示例";
        printWord(message,50);
    }
}
