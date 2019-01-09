import com.ultime5528.ntproperties.Callback;

public class K {

    public static int VAL = 25;
    public static double VAL2 = 23.2;
    public static boolean BOOL = true;
    
    public static class Inner {
        
        @Callback("updateBrightness")
        public static int BRIGHTNESS = 25;

        public static double VAL2 = 23.2;
        public static boolean BOOL = true;

        public static void updateBrightness() {
            System.out.println("Brightness was updated");
        }

    }

}