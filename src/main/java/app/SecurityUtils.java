package app;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SecurityUtils {

    private static final String SECRET_KEY = "KhoaBaoMatCuaTui";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void saveAccount(Object account, String filePath) throws Exception {
        String jsonString = mapper.writeValueAsString(account);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encryptedBytes = cipher.doFinal(jsonString.getBytes());
        String encryptedString = Base64.getEncoder().encodeToString(encryptedBytes);
        Files.write(Paths.get(filePath), encryptedString.getBytes());
    }

    public static <T> T loadAccount(String filePath, Class<T> valueType) throws Exception {
        String encryptedString = new String(Files.readAllBytes(Paths.get(filePath)));
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedString));
        return mapper.readValue(new String(decryptedBytes), valueType);
    }
}