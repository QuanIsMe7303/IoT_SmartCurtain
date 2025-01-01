#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include "DHT.h"

// WiFi và Firebase thông tin
#define WIFI_SSID "WIFI_NAME"
#define WIFI_PASSWORD "WIFI_PASSWORD"
#define API_KEY "YOUR_API_KEY"
#define DATABASE_URL "YOUR_DATABASE_URL"

// Cấu hình chân cảm biến và động cơ
#define LIGHT_SENSOR_PIN 34
#define DHT_PIN 27
#define MOTOR_IN1 12
#define MOTOR_IN2 13
#define MOTOR_ENABLE 14
#define LIMIT_SWITCH_OPEN 32
#define LIMIT_SWITCH_CLOSE 33

// Định danh thiết bị
String curtainID;
String roomID;

const int DHTTYPE = DHT11;
DHT dht(DHT_PIN, DHTTYPE);

const int freq = 5000;
const int resolution = 8;

unsigned long sendDataPrevMillis = 0;
bool signupOK = false;
int ldrData = 0;
float temperature = 0.0;
float humidity = 0.0;
const int LIGHT_THRESHOLD = 2000; // Ngưỡng ánh sáng
const int MOTOR_SPEED = 255;      // Tốc độ động cơ

// Biến cho chế độ điều khiển
bool isAutoMode = true;    // Mặc định là chế độ tự động
bool curtainState = false; // Đóng = false, Mở = true

// Khai báo Firebase, Auth và Config
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

void setup()
{
    Serial.begin(115200);
    dht.begin();

    // Thiết lập chân GPIO cho cảm biến và động cơ
    pinMode(LIGHT_SENSOR_PIN, INPUT);
    pinMode(MOTOR_IN1, OUTPUT);
    pinMode(MOTOR_IN2, OUTPUT);
    ledcAttachPin(MOTOR_ENABLE, 0);
    ledcSetup(0, freq, resolution);

    // Công tắc hành trình
    pinMode(LIMIT_SWITCH_OPEN, INPUT_PULLUP);
    pinMode(LIMIT_SWITCH_CLOSE, INPUT_PULLUP);

    // Kết nối WiFi
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to Wi-Fi");
    while (WiFi.status() != WL_CONNECTED)
    {
        Serial.print(".");
        delay(300);
    }
    Serial.println();
    Serial.print("Connected with IP: ");
    Serial.println(WiFi.localIP());
    Serial.println();

    // Cấu hình Firebase
    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;

    if (Firebase.signUp(&config, &auth, "", ""))
    {
        Serial.println("signUp OK");
        signupOK = true;
    }
    else
    {
        Serial.printf("%s\n", config.signer.signupError.message.c_str());
    }

    config.token_status_callback = tokenStatusCallback;
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);


    curtainID = generatePushID();
    Serial.println("Generated Device ID: " + curtainID);
    roomID = "";

    registerDevice();
}

void loop()
{
    if (Firebase.ready() && signupOK)
    {
        // Đọc chế độ điều khiển từ Firebase
        if (Firebase.RTDB.getBool(&fbdo, String("/curtains/") + curtainID + "/control/auto_mode"))
        {
            isAutoMode = fbdo.boolData();
        }

        // Đọc và xử lý giá trị cảm biến
        if (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0)
        {
            sendDataPrevMillis = millis();
            ldrData = analogRead(LIGHT_SENSOR_PIN);
            temperature = dht.readTemperature();
            humidity = dht.readHumidity();
            updateSensorData();
        }

        // Xử lý điều khiển rèm
        if (isAutoMode)
        {
            // Chế độ tự động: đóng rèm khi quá sáng
            if (ldrData > LIGHT_THRESHOLD && curtainState)
            {
                curtainState = false;
                controlCurtain(curtainState);
            }
            else if (ldrData <= LIGHT_THRESHOLD && !curtainState)
            {
                curtainState = true;
                controlCurtain(curtainState);
            }
        }
        else
        {
            // Chế độ thủ công: đọc trạng thái từ Firebase và điều khiển
            if (Firebase.RTDB.getBool(&fbdo, String("/curtains/") + curtainID + "/control/manual_state"))
            {
                bool manualState = fbdo.boolData();
                if (manualState != curtainState)
                {
                    curtainState = manualState;
                    controlCurtain(curtainState);
                }
            }
        }
    }
}

void registerDevice()
{
    Firebase.RTDB.setBool(&fbdo, String("/curtains/") + curtainID + "/status", false);
    Firebase.RTDB.setString(&fbdo, String("/curtains/") + curtainID + "/name", "New Curtain");
    Firebase.RTDB.setBool(&fbdo, String("/curtains/") + curtainID + "/auto_mode", true);
    Firebase.RTDB.setString(&fbdo, String("/curtains/") + curtainID + "/room_id", roomID);

    Firebase.RTDB.setInt(&fbdo, String("/sensors/") + roomID + "/sensor/light_value", ldrData);
    Firebase.RTDB.setFloat(&fbdo, String("/sensors/") + roomID + "/sensor/temperature", temperature);
    Firebase.RTDB.setFloat(&fbdo, String("/sensors/") + roomID + "/sensor/humidity", humidity);
}

void updateSensorData()
{
    // Cập nhật giá trị cảm biến lên Firebase
    Firebase.RTDB.setInt(&fbdo, String("/sensors/") + curtainID + "/sensor/light_value", ldrData);
    Firebase.RTDB.setFloat(&fbdo, String("/sensors/") + curtainID + "/sensor/temperature", temperature);
    Firebase.RTDB.setFloat(&fbdo, String("/sensors/") + curtainID + "/sensor/humidity", humidity);

    // In giá trị ra Serial Monitor
    Serial.printf("Light: %d, Temp: %.1f\u00B0C, Humidity: %.1f%%\n", ldrData, temperature, humidity);
}

void controlCurtain(bool openCurtain)
{
    if (openCurtain)
    {
        spinAnticlockwise(); // Mở rèm
        Firebase.RTDB.setBool(&fbdo, String("/curtains/") + curtainID + "/status", true);
    }
    else
    {
        spinClockwise(); // Đóng rèm
        Firebase.RTDB.setBool(&fbdo, String("/curtains/") + curtainID + "/status", false);
    }
}

void spinClockwise()
{
    digitalWrite(MOTOR_IN1, HIGH);
    digitalWrite(MOTOR_IN2, LOW);
    ledcWrite(0, MOTOR_SPEED);
    Serial.println("Curtain Closing");

    while (digitalRead(LIMIT_SWITCH_CLOSE) == HIGH)
    {
        delay(10);
        if (millis() - sendDataPrevMillis > 15000)
        {
            Serial.println("Closing timeout!");
            break;
        }
    }

    stopCurtain();
    Serial.println("Curtain Stop");
}

void spinAnticlockwise()
{
    digitalWrite(MOTOR_IN1, LOW);
    digitalWrite(MOTOR_IN2, HIGH);
    ledcWrite(0, MOTOR_SPEED);
    Serial.println("Curtain Opening");

    while (digitalRead(LIMIT_SWITCH_OPEN) == HIGH)
    {
        delay(10);

        if (millis() - sendDataPrevMillis > 15000)
        {
            Serial.println("Opening timeout!");
            break;
        }
    }

    stopCurtain();
    Serial.println("Curtain Stop");
}

void stopCurtain()
{
    digitalWrite(MOTOR_IN1, LOW);
    digitalWrite(MOTOR_IN2, LOW);
    ledcWrite(0, 0);
}

String generatePushID() {
    const char alphabet[] = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    char pushID[21];
    pushID[0] = '-';

    for (int i = 1; i < 20; i++) {
        pushID[i] = alphabet[random(0, sizeof(alphabet) - 1)];
    }

    pushID[20] = '\0'; // Kết thúc chuỗi
    return String(pushID);
}