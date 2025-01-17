
  

//import java.util.*;
//import javax.swing.*;
import processing.core.*;
import com.fuzzylite.*;
import com.fuzzylite.defuzzifier.*;
import com.fuzzylite.norm.s.*;
import com.fuzzylite.norm.t.*;
import com.fuzzylite.rule.*;
import com.fuzzylite.term.*;
import com.fuzzylite.variable.*;
import controlP5.*;

// Listener class to handle events on the dial control
class TargetTempListener implements ControlListener {
      private int targetTemp;
      TargetTempListener(){this.targetTemp = 18;}
      public void controlEvent(ControlEvent theEvent) {
          targetTemp = (int)theEvent.getController().getValue();
      }
      public int getTargetTemp(){return targetTemp;}
    }
// Listener class to handle events on the checkbox control
class CheckboxListener implements ControlListener {
      private int active;
      CheckboxListener(){this.active = 0;}
      public void controlEvent(ControlEvent theEvent) {
          active = (int)theEvent.getController().getValue();
      }
      public boolean isActive(){return active == 1;}
    }

public class FuzzyAirconController extends PApplet{

    private final int INITIAL_TEMP = 18;

    // Step vars to control perlin noise
    private float t1 = (float) 3;

    // Current room temperature
    private float roomTemp;
    private float targetTemp;

    // AC action (-/+)
    private float acCommand;

    // Setup some colours
    private int blue = color(0,174,253);
    private int red = color(255,0,0);
    private int black = color(0,0,0);
    private int grey = color(240,240,240);
    //private int white = color(255,255,255);
    private int green = color(0,204,102);

    // Fuzzy Logic objects
    private Engine engine;
    private InputVariable inputVariable1;
    private InputVariable inputVariable2;
    private OutputVariable outputVariable;
    private RuleBlock ruleBlock;

    private ControlP5 cp5;
    private TargetTempListener targetTempListener;
    private CheckboxListener acPowerListener;
    private Chart tempChart;

    public void settings(){
        size(800, 360);
    }
    public void createFuzzyEngine(){
        // Create the engine
        engine = new Engine();
        engine.setName("FuzzyAirconController");

        inputVariable1 = new InputVariable();
        inputVariable1.setEnabled(true);
        inputVariable1.setName("temperature");
        inputVariable1.setRange(0, 40.00);//added adam

        // TODO
        // Set the range and terms for the room temperature input
        //added adam
        inputVariable1.addTerm(new Triangle("toocold",0, 0, 10));
        inputVariable1.addTerm(new Triangle("cold",5, 10, 18));
        inputVariable1.addTerm(new Triangle("warm",15, 20, 25 ));
        inputVariable1.addTerm(new Triangle("hot", 20, 30, 35));
        inputVariable1.addTerm(new Triangle("toohot", 30, 40,40));
        engine.addInputVariable(inputVariable1);

        inputVariable2 = new InputVariable();
        inputVariable2.setEnabled(true);
        inputVariable2.setName("target");
        inputVariable2.setRange(15, 35);
        
        // TODO
        // Set the range and terms for the target temperature input
        inputVariable2.setRange(0, 40);
        // Add each term for the Linguistic variable
        inputVariable2.addTerm(new Triangle("vlow", 0 , 5, 10));
        inputVariable2.addTerm(new Trapezoid("low", 5 ,10 , 12.5, 17.5));//i want colder 
        inputVariable2.addTerm(new Trapezoid("normal",12.5, 17.5 ,22.5,25 ));//i want normal
        inputVariable2.addTerm(new Trapezoid("high", 22.5, 25, 30,35));
        inputVariable2.addTerm(new Trapezoid("vhigh", 30 , 35, 37.5,40));//i want hot
        // Add the variable to the fuzzy engine
        engine.addInputVariable(inputVariable2);


        //engine.addInputVariable(inputVariable2);

        outputVariable = new OutputVariable();
        outputVariable.setEnabled(true);
        outputVariable.setName("command");
        outputVariable.setRange(-10, 10);
        // How should the rules be accumulated
        outputVariable.fuzzyOutput().setAccumulation(new Maximum());
        // How will the output be Defuzzified?
        outputVariable.setDefuzzifier(new Centroid(200));
        outputVariable.setDefaultValue(0.000);
        outputVariable.setLockValidOutput(false);
        outputVariable.setLockOutputRange(false);
        //what should the ac do
        outputVariable.addTerm(new Trapezoid("vlow",-10, -8.5,-6.5, -4.5));
        outputVariable.addTerm(new Trapezoid("low",-6.5, -4.5, -2,0));
        outputVariable.addTerm(new Triangle("normal",-1 , 0 , 1));
        outputVariable.addTerm(new Trapezoid("high", 0 , 2 , 4.5, 6.5));
        outputVariable.addTerm(new Trapezoid("vhigh",4.5, 6.5, 8.5, 10));

        // Add the variable to the fuzzy engine
        engine.addOutputVariable(outputVariable);

        // TODO
        // Set the range and terms for the command output
        

        engine.addOutputVariable(outputVariable);




        // Setup the inference rules
        ruleBlock = new RuleBlock();
        ruleBlock.setEnabled(true);
        ruleBlock.setName("Rule Block");
        // Set up fuzzy functions for AND, OR and NOT
        ruleBlock.setConjunction(new Minimum());
        ruleBlock.setDisjunction(new Maximum());
        ruleBlock.setActivation(new Minimum());
        // Add the rules as follows

        //low end ( 0 to 18) 
        ruleBlock.addRule(Rule.parse("if (temperature is warm) and (target is low) then command is low", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is warm) and (target is normal) then command is normal", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is cold) and (target is normal) then command is vhigh", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is cold) and (target is low) then command is normal", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is cold) and (target is vlow) then command is vlow", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is toocold) and (target is vlow) then command is normal", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is toocold) and (target is low) then command is high", engine));
        //high end (19 to 40)#
        ruleBlock.addRule(Rule.parse("if (temperature is warm) and (target is high) then command is high", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is hot) and (target is normal) then command is vlow", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is hot) and (target is high) then command is normal", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is hot) and (target is vhigh) then command is vhigh", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is toohot) and (target is high) then command is low", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is toohot) and (target is normal) then command is low", engine));
        
        ruleBlock.addRule(Rule.parse("if (temperature is cold) and (target is vhigh) then command is vhigh", engine));
        ruleBlock.addRule(Rule.parse("if (temperature is toohot) and (target is low) then command is vlow", engine));


        

        // ruleBlock.addRule(Rule.parse("if (temperature is toocold) and (target is high) then command is high", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is toocold) and (target is normal) then command is high", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is toocold) and (target is low) then command is normal", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is cold) and (target is low) then command is low", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is cold) and (target is normal) then command is high", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is cold) and (target is high) then command is high", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is warm) and (target is low) then command is low", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is warm) and (target is normal) then command is normal", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is warm) and (target is high) then command is high", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is hot) and (target is low) then command is low", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is hot) and (target is normal) then command is normal", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is hot) and (target is high) then command is high", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is toohot) and (target is low) then command is low", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is toohot) and (target is normal) then command is low", engine));
        // ruleBlock.addRule(Rule.parse("if (temperature is toohot) and (target is high) then command is normal", engine));
       
        // TODO - Add the rest of the rules - see lab sheet


        // Add the rule block to the fuzzy engine
        engine.addRuleBlock(ruleBlock);
    }
    public void setup(){
        roomTemp = INITIAL_TEMP;
        acCommand = 0f;
        // Set the fuzzy logic engine including inputs, outputs and rules
        createFuzzyEngine();

        // Create a ControlP5 object for the user interface
        cp5 = new ControlP5(this);

        // Create the target temperature dial
        cp5.addKnob("Target Temperature")
            .setRange(5,35) // Most room thermostats have this range - don't change
            .setValue(INITIAL_TEMP)
            .setPosition(180,180)
            .setRadius(65)
            .setColorCaptionLabel(black)
            .setDragDirection(Knob.VERTICAL)
            ;

        // Create a new listener for the target temperature slider
        targetTempListener = new TargetTempListener();
        // Add a listener so we can receive slider events
        cp5.getController("Target Temperature").addListener(targetTempListener);

        cp5.addCheckBox("AC Checkbox")
            .setPosition(210, 150)
            .setColorForeground(color(0))
            .setColorActive(blue)
            .setColorLabel(color(0))
            .setSize(20, 20)
            .addItem("AC On/Off", 0)
            ;
        acPowerListener = new CheckboxListener();
        // Add a listener so we can receive slider events
        cp5.getController("AC On/Off").addListener(acPowerListener);

        tempChart = cp5.addChart("Temperature")
                   .setPosition(350, 160)
                   .setColorCaptionLabel(black)
                   .setSize(400, 150)
                   .setRange(-15, 45)
                   .setColorBackground(grey)
                   .setLabelVisible(true)
                   .setView(Chart.LINE) // use Chart.LINE, Chart.PIE, Chart.AREA, Chart.BAR_CENTERED
                   ;
        // Setup the dataset for outside temperature
        tempChart.addDataSet("Outside");
        tempChart.setData("Outside", new float[2000]);
        // Setup the dataset for room temperature
        tempChart.addDataSet("Room");
        tempChart.setData("Room", new float[2000]);
        tempChart.setColors("Room", red);
        // Setup the dataset for target temperature
        tempChart.addDataSet("Target");
        tempChart.setData("Target", new float[2000]);
        tempChart.setColors("Target", green);

    }

    private void drawThermometer(){
        // Draw the thermometer
        stroke(0);
        line(100,300,100,150); // Left side
        line(105,300,105,150); // Right side
        line(100,300,105,300); // Bottom
        fill(red);
        ellipse(103,310,19,19);

        // Draw the tick marks and labels
        fill(black);
        line(90, 300, 100, 300); // Zero tick
        text("0",80,305);
        line(95, (int)(300-18.75), 100, (int)(300-18.75)); // 5 tick
        line(90, (int)(300-37.5), 100, (int)(300-37.5)); // 10 tick
        text("10",73,(int)(300-37.5+5));
        line(95,(int)(300-56.25), 100, (int)(300-56.25)); // 15 tick

        line(90, 300-75, 100, 300-75); // 20 tick
        line(95, (int)(300-93.75), 100, (int)(300-93.75)); // 25 tick
        text("20",73,(int)(300-75+5));
        line(90, (int)(300-112.5), 100, (int)(300-112.5)); // 30 tick
        line(95, (int)(300-131.25), 100, (int)(300-131.25)); // 35 tick
        text("30",73,(int)(300-112.5+5));
        line(90, 300-150, 100, 300-150); // 40 tick
        text("40",73,(int)(300-150+5));

    }
    private void drawTempLevel(float lev){
        // No outline for the water
        noStroke();
        // Set the fill color
        fill(red);
        // Draw the rect for the temp level
        double ratio = 150*(lev / 40);
        rect(101,(int)(300-ratio),4,(int)(ratio+1));
    }
    private void drawInfo(float ot, float rt, float tt, float ac){
        // Output the room temp and target temp
        fill(black);
        text("Outside Temp: " + ot,50,35);
        text("Room Temp: " + rt,50,50);
        text("Target Temp: " + tt,50,65);
        text("AC Command: " + ac,50,80);

        // Chart legend
        fill(blue);
        rect(350,145, 10,10);
        fill(black);
        text("Outside", 365, 155);
        // Chart legend
        fill(red);
        rect(420,145, 10,10);
        fill(black);
        text("Room", 435, 155);
        // Chart legend
        fill(green);
        rect(480,145, 10,10);
        fill(black);
        text("Target", 495, 155);

    }
    private float fuzzyACEvaluate(float rt, float tt){
        //System.out.println(rt);
        // Load the input variables
        inputVariable1.setInputValue(rt); // room temp
        inputVariable2.setInputValue(tt); // target temp
        // Run the engine
        engine.process();
        // Get the output
        return (float)(outputVariable.defuzzify());
    }

    // Run the system
    public void drawSystem(){
        // Clear the background
        background(255);

        // Draw all the static visual components
        drawThermometer();

        // Constant change
        float outsideTemp = noise(t1);
        // Map the demand to a value between -1 and 1.5
        outsideTemp = map(outsideTemp,0f,1f,-10.0f,40f);
        // Calculate the difference between the outside temp and room temp
        // We will use a scaling factor to make the change in temp gradual
        float tempDelta = (roomTemp - outsideTemp) * 0.001f;

        // Get the target temperature
        targetTemp = targetTempListener.getTargetTemp();

        // Run the fuzzy engine with inputs and get controller output
        if (acPowerListener.isActive()){

            // TODO: uncomment the call to fuzzyACEvaluate below
            // Run the fuzzy controller on our inputs and get an output
            acCommand = fuzzyACEvaluate(roomTemp, targetTemp);

            // Apply the pump action to the current level
            // new roomTemp will be affected by the delta (room - outside) and the AC fuzzy output
            roomTemp = (roomTemp  - tempDelta + (acCommand * 0.01f));
        }
        else if (!acPowerListener.isActive()){
            // If the AC is not active then the room temp will
            // only be affected the outside temp
            roomTemp = roomTemp  - tempDelta;
        }

        // Update the temperature level on screen
        drawTempLevel(roomTemp);

        // Draw the instrumentation panel
        drawInfo(outsideTemp, roomTemp, targetTemp, acCommand);

        // Increment time step for Perlin noise
        t1 += 0.001;

        // Push the data from this time step on to the live graph
        tempChart.push("Outside", outsideTemp);
        tempChart.push("Room", roomTemp);
        tempChart.push("Target", targetTemp);

    }

    // Draw each frame of animation
    public void draw(){
        drawSystem();
    }

    // Main method
    public static void main(String[] args) {
        PApplet.main("FuzzyAirconController");

    }

}
