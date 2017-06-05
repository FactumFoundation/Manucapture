int lastSwitchInput = HIGH;

void setup(){
 Serial.begin(9600); 
 pinMode(2,INPUT_PULLUP); 
  
}

void loop(){
  int switchInput = digitalRead(2);
  
  if(switchInput == LOW && lastSwitchInput == HIGH)
  {
   Serial.println("t");
   delay(200);
  } 
  lastSwitchInput = switchInput;
}
