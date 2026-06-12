# Design

# Gamification Specifications – Entrepreneurial Simulation Game

## 1\. Gameplay Objective

## The game system must provide an interactive experience enabling a player (a 12-year-old child) to **discover and experiment with entrepreneurship concepts** through a **progressive, decision-based simulation**.

This game is for 12yo to discover Entrepreneurship by playing a roleplay where AI is the game master. 

Progression relies on:

- Sequential **decision-making**   
- **Measurable consequences**  
- A **dynamic evolution of a simulated company**

## 2\. Core Gameplay Loop

## The gameplay is built around a **loop after Game initialization :**

1. ## Event generation (AI picks a theme and looks at the previous choices to select consequences  ) 

2. ## Situation presentation

3. ## Player decision (choice or custom input)

4. ## Decision interpretation (narrative explanation, Indicator updates, GameOver detection)

5. ## Transition to next event

## **3\. Initialization Phase**

### **3.1 Character Selection**

* ## The player selects an avatar/character.

* ## This choice may influence:

  * ## Narrative tone

  * ## Optional implicit bonuses/maluses

### **3.2 Company Creation**

* ## The player chooses: 

  * ## either a type of company from a predefined list

  * ## or a custom option (that follow the Guardrails)

* ## This choice serves to: 

  * ## contextualize events

  * ## guide scenario generation

## **4\. Event System**

### **4.1 Event Generation**

## Each event is dynamically generated based on:

* ## A **theme** selected from a predefined list, such as: 

  * ## Financial

  * ## Ecological

  * ## AI impact

  * ## Political/regulatory

  * ## Aggressive Competition

  * ## Employee management

  * ## Growth and scaling

  * Customers Relation  
  * Partnerships & Negotiation  
  * International Expansion  
  * Unexpected incident linked to the company (follow guardrails)  
  * Communication & Media  
  * Innovation & Product Development

* ## The player’s **decision history (it would feel like a good scenario design if previous decision brings consequences)** 

* ## The **current state of indicators**

* ## **Illustration of the theme** 

* ## **Who is presenting the problem (who we chat with)** 

* ## **The AI adds a lexicon with all technical terms and their meaning (to be added in the UI for educational purposes)** 

## ---

### **4.2 Event Structure**

## Each event consists of three components:

#### **1\. Situation**

* ## A narrative description of a problem or challenge

* ## Contextualized using:

  * ## company type

  * ## previous decisions

  * ## selected theme

#### **2\. Proposed Solutions**

* ## A list of predefined options (minimum 2–3)

* ## Each option represents a different strategic approach

* ## The player can also:

  * ## **enter a custom solution** (free text). It must respect the Guardrails.

#### **3\. Contextual Help (Optional)**

* ## The player may request: 

  * ## an explanation of the situation

  * ## an estimation of possible impacts

* ## This help must: 

  * ## guide thinking

  * ## avoid directly revealing the optimal solution

## ---

## **5\. Decision System**

### **5.1 Player Inputs**

* ## Selection of a predefined option

* ## Creation of a custom response

### **5.2 Decision Processing**

## The system must:

* ## Interpret the decision (selected or written)

* ## Map it to effects on indicators

* ## Generate a coherent outcome

## ---

## **6\. Indicator System**

### **6.1 Core Indicators**

## The company is defined by three variables:

* ## 💰 **Money**

* ## 🌱 **Ecology**

* ## 🤝 **Image (ethics / reputation)**

### **6.2 Management Rules**

* ## Each indicator value: 

  * ## ranges from **0 to 10**

  * ## starts at **3**

* ## Each decision impacts indicators: 

  * ## positively

  * ## negatively

  * ## or neutrally

### **6.3 Impact Logic**

* ## Effects must be:

  * ## contextually consistent

  * ## potentially conflicting (e.g., \+money but \-ecology)

## ---

## **7\. Progression and Difficulty**

### **7.1 Number of Events**

* ## A session contains a maximum of **15 events**

### **7.2 Difficulty Scaling**

## Events must:

* ## gradually increase in complexity

* ## involve more variables

* ## introduce more nuanced dilemmas

## ---

## **8\. End Conditions**

### **8.1 Game Over**

## The game ends immediately if:

* ## any indicator reaches **0**

### **8.2 Normal Completion**

## The game ends after:

* ## the 15th event (if no prior failure occurs)

## ---

## **9\. Player Feedback**

## After each decision:

* ## display consequences:

  * ## indicator changes

  * ## narrative explanation

* ## show the company’s progression

## ---

## **10\. Implicit Player Objective**

## The player must:

* ## maintain balance across indicators

* ## ensure company survival and growth

* ## understand trade-offs inherent to entrepreneurship

## ---

## **✅ Summary of Core Mechanics**

* ## Loop: **Situation → Choice → Consequence**

* ## Scoring system: **3 dynamic indicators**

* ## Progression: **up to 15 events**

* ## Increasing complexity

* ## Dual input system (choices \+ free text)

* ## Integrated pedagogical assistance

* ## Clear success and failure conditions

- 

# Game State

| Company Context | Name, Assets |
| :---- | :---- |
| Decision history | List of problems and selected solutions |
| Scores | Economy  Ecology Public Image |
| Problem themes | Money / Ecology / Politics / IA / Competitor |
|  |  |

# Guardrails 

- Language fits a 12yo  
- No invalid themes  
- French  
- Stay focuses on the game 

# UI

Chat box : problem are presented by Investors/Employees/ … depending on the context  
Scores: a small icon grows to visually represent the score (0-10) 

Theme BD

# Codebase

### Frontend : Angular

Main frame: chat   
Expose state of the startup

### Backend : SpringBoot

- UI Asset generation

### Packaging : Docker

We are going to package this app into a docker and host it on cloud. 

# Prompts

| Event Generation  Variables:  Company context |  |
| :---- | :---- |
|  |  |
|  |  |
|  |  |
|  |  |
|  |  |
|  |  |
|  |  |
|  |  |

