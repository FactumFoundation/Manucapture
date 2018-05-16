int footSwitchPin = 12;
int footSwitchValue = HIGH;
int prevFootSwitchValue = HIGH;
int footSwitchCounter = 0;

boolean triggerCameras = false;

int IDLE_STATE = 0;
int DETECTING_ON_STATE = 1;
int TRIGGERING_STATE = 2;
int DETECTING_OFF_STATE = 3;
int state = IDLE_STATE;

int camPin_1 = 8;
int camPin_2 = 9;

void setup() {
  // put your setup code here, to run once:
  pinMode(footSwitchPin, INPUT_PULLUP);
  pinMode(camPin_1, OUTPUT);
  pinMode(camPin_2, OUTPUT);
  Serial.begin(9600);

  digitalWrite(camPin_1,HIGH);
  digitalWrite(camPin_2,HIGH);

}

void loop() {
  
  // put your main code here, to run repeatedly:
  footSwitchValue = digitalRead(footSwitchPin);


  if(state == IDLE_STATE) {
    if(prevFootSwitchValue==HIGH && footSwitchValue==LOW){
      state = DETECTING_ON_STATE;
      footSwitchCounter = 0;
    } 
  } else if(state == DETECTING_ON_STATE) {
    if(prevFootSwitchValue==LOW && footSwitchValue==LOW ) {
      footSwitchCounter = footSwitchCounter+1;
      if(footSwitchCounter > 10){
        state = TRIGGERING_STATE;
      }
    } else {
      state = IDLE_STATE;
    }
  } else if(state == TRIGGERING_STATE) {
      digitalWrite(camPin_1,LOW);
      delay(400);
      digitalWrite(camPin_1,HIGH);
      digitalWrite(camPin_2,LOW);
      delay(400);
      digitalWrite(camPin_2,HIGH);
      state = DETECTING_OFF_STATE;
      footSwitchCounter = 0;
  } else if(state == DETECTING_OFF_STATE) {
    if(prevFootSwitchValue==HIGH && footSwitchValue==HIGH ) {
      footSwitchCounter = footSwitchCounter+1;
      if(footSwitchCounter > 10){
        state = IDLE_STATE;
        delay(2000);
      }
    }
  }

  prevFootSwitchValue = footSwitchValue;

}
