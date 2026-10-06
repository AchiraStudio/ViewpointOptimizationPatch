import viewpoint.platform.ShaderPacks;

public class TestShaderPacks {
    public static void main(String[] args) {
        try {
            System.out.println("Testing ShaderPacks.library()...");
            ShaderPacks.library(0);
            System.out.println("SUCCESS! ShaderPacks loaded properly.");
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
