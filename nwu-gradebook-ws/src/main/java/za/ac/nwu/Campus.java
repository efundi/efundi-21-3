package za.ac.nwu;

public class Campus {

    public static String getNumber(String code) {
        String number = null;
        if (code.equals("P")) {
        	number = "1";
        }
        else if (code.equals("V")) {
        	number = "2";
        }
        else if (code.equals("M")) {
        	number = "9";
        }
        return number;
    }
}
