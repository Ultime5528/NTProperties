import com.ultime5528.ntproperties.Callback;

public class K {

    @Callback("updateBool")
    public static boolean BOOL = true;
    
    public static void updateBool() {
        System.out.println("BOOL was updated");
    }

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