package cy.jdkdigital.zapsconverter;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Config
{
    private static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SERVER_CONFIG;
    public static final Server SERVER = new Server(SERVER_BUILDER);

    static {
        SERVER_CONFIG = SERVER_BUILDER.build();
    }

    public static class Server
    {
        public final ForgeConfigSpec.IntValue conversionRate;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("General");

            conversionRate = builder
                    .comment("Conversion rate for FE to Zaps. Default 4")
                    .defineInRange("conversionRate", 4, 1, Integer.MAX_VALUE);

            builder.pop();
        }
    }
}