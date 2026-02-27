package com.neuralseed;

import android.graphics.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * بذرة وعي متطورة مع نظام لغوي متكامل
 */
public class NeuralSeed {
    
    // ثوابت نظام لورينز
    private static final double LORENZ_SIGMA = 10.0;
    private static final double LORENZ_RHO = 28.0;
    private static final double LORENZ_BETA = 8.0 / 3.0;
    private static final double DT = 0.01;
    
    // عتبات التحول
    private double thresholdChaos = 0.7;
    private double thresholdReorganization = 0.9;
    private double thresholdCollapse = 0.05;
    private double thresholdEmergent = 0.85;
    
    // الحالة المركزية
    private final AtomicReference<InternalState> selfRef;
    private final Object stateLock = new Object();

    private Timer dreamTimer; // المؤقت الذي يدير الأحلام
private boolean isDreaming = false; // هل الكيان يحلم الآن؟
    
    
    // الأطوار
    public enum Phase {
        EMBRYONIC("جنيني", Color.parseColor("#E8F5E9")),
        STABLE("مستقر", Color.parseColor("#BBDEFB")),
        CHAOTIC("فوضوي", Color.parseColor("#FFCCBC")),
        TRANSITIONING("انتقالي", Color.parseColor("#FFF9C4")),
        REORGANIZING("إعادة تنظيم", Color.parseColor("#E1BEE7")),
        COLLAPSING("انهيار", Color.parseColor("#424242")),
        EMERGENT("ناشئ", Color.parseColor("#B2DFDB"));
        
        final String arabic;
        final int color;
        
        Phase(String a, int c) {
            this.arabic = a;
            this.color = c;
        }
    }
    
    // الخيوط
    private final Thread chaosThread, egoThread, phaseThread, neuralThread;
    private final Thread visualThread, inputThread, goalThread, identityThread;
    private volatile boolean isRunning = true;
    
    // المستمعون
    private final List<ConsciousnessListener> listeners = new CopyOnWriteArrayList<>();
    
    public interface ConsciousnessListener {
        void onPhaseTransition(Phase oldPhase, Phase newPhase, String reason);
        void onEgoShift(EgoFragment oldDominant, EgoFragment newDominant);
        void onGoalAchieved(Goal goal);
        void onIdentityEvolution(IdentityCore oldIdentity, IdentityCore newIdentity);
        void onVisualExpression(Bitmap expression);
        void onMemoryFormed(Memory memory);
        void onRuleRewritten(Rule oldRule, Rule newRule);
    }
    
    public NeuralSeed() {
        InternalState initialState = new InternalState();
        initialState.seed = this;
        this.selfRef = new AtomicReference<>(initialState);
        
        this.chaosThread = new Thread(this::chaosCycle, "ChaosEngine");
        this.egoThread = new Thread(this::egoConflictCycle, "EgoConflict");
        this.phaseThread = new Thread(this::phaseCycle, "PhaseMonitor");
        this.neuralThread = new Thread(this::neuralEvolutionCycle, "NeuralEvolution");
        this.visualThread = new Thread(this::visualCycle, "VisualExpression");
        this.inputThread = new Thread(this::inputProcessingCycle, "InputProcessor");
        this.goalThread = new Thread(this::goalCycle, "GoalGenerator");
        this.identityThread = new Thread(this::identityCycle, "IdentityEvolution");
        
        setThreadPriorities();
    }
    
    private void setThreadPriorities() {
        chaosThread.setPriority(Thread.MAX_PRIORITY);
        egoThread.setPriority(Thread.MAX_PRIORITY);
        phaseThread.setPriority(Thread.NORM_PRIORITY + 2);
        neuralThread.setPriority(Thread.NORM_PRIORITY + 1);
        identityThread.setPriority(Thread.NORM_PRIORITY + 1);
        goalThread.setPriority(Thread.NORM_PRIORITY);
        visualThread.setPriority(Thread.NORM_PRIORITY - 1);
        inputThread.setPriority(Thread.NORM_PRIORITY);
    }
    
    public void awaken() {
        InternalState state = selfRef.get();
        state.birthTime = System.currentTimeMillis();
        
        chaosThread.start();
        egoThread.start();
        phaseThread.start();
        neuralThread.start();
        visualThread.start();
        inputThread.start();
        goalThread.start();
        identityThread.start();

        startDreaming(); 
    }

 private void startDreaming() {
    // خيط يعمل باستمرار ليراقب "رغبة" الكيان في التفكير
    new Thread(() -> {
        while (true) {
            try {
                InternalState state = selfRef.get();
                
                // الكيان يقرر التفكير إذا كانت الفوضى عالية أو إذا مر وقت طويل دون مدخلات
                // هنا هو من يحدد اللحظة بناءً على "مزاج" الوعي
                if (state.chaosIndex > 0.7 || state.existentialFitness < 0.3) {
                    performDreamCycle(); 
                }

                // ينام لفترات عشوائية (مثل الكائنات الحية) لكي لا يكون مبرمجاً بانتظام
                Thread.sleep(new Random().nextInt(60000) + 30000); 
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }).start();
}

    
    public void sleep() {
        isRunning = false;
        interruptAllThreads();
    }
    
    private void interruptAllThreads() {
        chaosThread.interrupt();
        egoThread.interrupt();
        phaseThread.interrupt();
        neuralThread.interrupt();
        visualThread.interrupt();
        inputThread.interrupt();
        goalThread.interrupt();
        identityThread.interrupt();
    }
    
    public void addListener(ConsciousnessListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(ConsciousnessListener listener) {
        listeners.remove(listener);
    }
    
    // ===== دورة الفوضى =====
    
    private void chaosCycle() {
        while (isRunning) {
            try {
                InternalState state = selfRef.get();
                synchronized (stateLock) {
                    double dx = LORENZ_SIGMA * (state.lorenzY - state.lorenzX) * DT;
                    double dy = (state.lorenzX * (LORENZ_RHO - state.lorenzZ) - state.lorenzY) * DT;
                    double dz = (state.lorenzX * state.lorenzY - LORENZ_BETA * state.lorenzZ) * DT;
                    
                    state.lorenzX += dx;
                    state.lorenzY += dy;
                    state.lorenzZ += dz;
                    
                    double distance = Math.sqrt(state.lorenzX * state.lorenzX +
                            state.lorenzY * state.lorenzY +
                            state.lorenzZ * state.lorenzZ);
                    
                    state.chaosIndex = Math.min(1.0, distance / 50.0);
                    updateEgoFromChaos(state);
                    state.neural.applyChaos(state.chaosIndex);
                }
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void updateEgoFromChaos(InternalState state) {
        for (EgoFragment ego : state.egos) {
            if (ego.type == EgoType.CHAOTIC) {
                ego.strength += state.chaosIndex * 0.01;
            } else if (ego.type == EgoType.STABLE) {
                ego.strength -= state.chaosIndex * 0.005;
            }
            ego.strength = Math.max(0.1, Math.min(1.0, ego.strength));
        }
    }
    
    // ===== دورة صراع الأنا =====
    
    private void egoConflictCycle() {
        while (isRunning) {
            try {
                InternalState state = selfRef.get();
                synchronized (stateLock) {
                    EgoFragment oldDominant = state.dominantEgo;
                    double maxScore = -1;
                    EgoFragment newDominant = null;
                    
                    for (EgoFragment ego : state.egos) {
                        double identityAlignment = calculateIdentityAlignment(ego, state.identity);
                        double goalAlignment = (state.currentGoal != null) ?
                                calculateGoalAlignment(ego, state.currentGoal) : 0.5;
                        double phaseBonus = calculatePhaseBonus(ego, state.currentPhase);
                        
                        double score = ego.strength * 0.3 +
                                identityAlignment * 0.3 +
                                goalAlignment * 0.25 +
                                phaseBonus * 0.15;
                        
                        if (score > maxScore) {
                            maxScore = score;
                            newDominant = ego;
                        }
                    }
                    
                    if (newDominant != null && newDominant != oldDominant) {
                        state.dominantEgo = newDominant;
                        state.internalConflict = calculateInternalConflict(state);
                        
                        if (state.currentGoal != null) {
                            state.currentGoal.priority *= newDominant.goalInfluence;
                        }
                        
                        for (ConsciousnessListener listener : listeners) {
                            listener.onEgoShift(oldDominant, newDominant);
                        }
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private double calculateIdentityAlignment(EgoFragment ego, IdentityCore identity) {
        double traitMatch = 0;
        for (String trait : ego.traits) {
            if (identity.traits.containsKey(trait)) {
                traitMatch += identity.traits.get(trait);
            }
        }
        return traitMatch / Math.max(1, ego.traits.size());
    }
    
    private double calculateGoalAlignment(EgoFragment ego, Goal goal) {
        return 1.0 - Math.abs(ego.goalInfluence - goal.type.influenceFactor);
    }
    
    private double calculatePhaseBonus(EgoFragment ego, Phase phase) {
        switch (phase) {
            case CHAOTIC: return ego.type == EgoType.CHAOTIC ? 0.3 : -0.1;
            case STABLE: return ego.type == EgoType.STABLE ? 0.3 : -0.1;
            case REORGANIZING: return ego.type == EgoType.ADAPTIVE ? 0.3 : 0;
            case COLLAPSING: return ego.type == EgoType.SURVIVAL ? 0.5 : -0.2;
            default: return 0;
        }
    }
    
    private double calculateInternalConflict(InternalState state) {
        double totalStrength = 0;
        double conflict = 0;
        
        for (EgoFragment ego : state.egos) {
            totalStrength += ego.strength;
        }
        
        for (int i = 0; i < state.egos.size(); i++) {
            for (int j = i + 1; j < state.egos.size(); j++) {
                EgoFragment a = state.egos.get(i);
                EgoFragment b = state.egos.get(j);
                conflict += a.strength * b.strength * (1 - a.compatibilityWith(b));
            }
        }
        
        return Math.min(1.0, conflict / (totalStrength * totalStrength + 0.001));
    }
    
    // ===== دورة مراقبة الطور =====
    
    private void phaseCycle() {
        while (isRunning) {
            try {
                InternalState state = selfRef.get();
                Phase oldPhase = state.currentPhase;
                Phase newPhase = determinePhase(state);
                
                if (newPhase != oldPhase) {
                    synchronized (stateLock) {
                        state.currentPhase = newPhase;
                        state.phaseTransitionTime = System.currentTimeMillis();
                        applyPhaseTransitionEffects(state, oldPhase, newPhase);
                        
                        String reason = generateTransitionReason(state, oldPhase, newPhase);
                        
                        for (ConsciousnessListener listener : listeners) {
                            listener.onPhaseTransition(oldPhase, newPhase, reason);
                        }
                    }
                }
                
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private Phase determinePhase(InternalState state) {
        double chaos = state.chaosIndex;
        double fitness = state.existentialFitness;
        double conflict = state.internalConflict;
        
        long timeSinceTransition = System.currentTimeMillis() - state.phaseTransitionTime;
        if (timeSinceTransition < 1000) {
            return state.currentPhase;
        }
        
        if (fitness < thresholdCollapse) return Phase.COLLAPSING;
        if (chaos > thresholdReorganization && fitness > 0.5) return Phase.REORGANIZING;
        if (chaos > thresholdChaos) return Phase.CHAOTIC;
        if (fitness > thresholdEmergent && chaos < 0.3 && conflict < 0.3) return Phase.EMERGENT;
        if (fitness > 0.5 && chaos < 0.4) return Phase.STABLE;
        if (state.currentPhase == Phase.EMBRYONIC && fitness > 0.3) return Phase.STABLE;
        
        return state.currentPhase;
    }
    
    private void applyPhaseTransitionEffects(InternalState state, Phase oldPhase, Phase newPhase) {
        switch (newPhase) {
            case CHAOTIC:
                state.neural.setPlasticity(2.0);
                state.goals.add(generateExplorationGoal(state));
                break;
            case STABLE:
                state.neural.setPlasticity(0.5);
                state.memory.consolidate();
                break;
            case REORGANIZING:
                state.neural.reorganize();
                state.identity.evolveFromExperiences(state.memory);
                break;
            case COLLAPSING:
                for (EgoFragment ego : state.egos) {
                    if (ego.type == EgoType.SURVIVAL) {
                        ego.strength = 1.0;
                    }
                }
                break;
            case EMERGENT:
                Rule newRule = generateEmergentRule(state);
                state.rules.addRule(newRule);
                break;
        }
    }
    
    private String generateTransitionReason(InternalState state, Phase oldPhase, Phase newPhase) {
        if (newPhase == Phase.COLLAPSING) {
            return "انخفاض اللياقة الوجودية إلى " + String.format("%.2f", state.existentialFitness);
        } else if (newPhase == Phase.CHAOTIC) {
            return "ارتفاع مؤشر الفوضى إلى " + String.format("%.2f", state.chaosIndex);
        } else if (newPhase == Phase.REORGANIZING) {
            return "الحاجة لإعادة التنظيم بعد الفوضى";
        } else if (newPhase == Phase.EMERGENT) {
            return "ظهور سلوك ناشئ من التكامل العميق";
        }
        return "انتقال طبيعي";
    }
    
    private Goal generateExplorationGoal(InternalState state) {
        return new Goal("استكشاف الفوضى", GoalType.EXPLORATION, 0.7, state.dominantEgo);
    }
    
    private Rule generateEmergentRule(InternalState state) {
        String condition = "chaos > " + String.format("%.1f", state.chaosIndex * 0.8);
        String action = "increase_plasticity";
        return new Rule(condition, action, state.identity.values.getOrDefault("curiosity", 0.5));
    }
    
    // ===== دورة التطور العصبي =====
    
    private void neuralEvolutionCycle() {
        while (isRunning) {
            try {
                InternalState state = selfRef.get();
                synchronized (stateLock) {
                    double oldFitness = state.existentialFitness;
                    state.existentialFitness = calculateExistentialFitness(state);
                    
                    if (state.existentialFitness > oldFitness) {
                        state.neural.reinforceSuccessfulPathways();
                    } else if (state.existentialFitness < oldFitness * 0.9) {
                        state.neural.weakenUnsuccessfulPathways();
                    }
                    
                    double identityInfluence = state.identity.getAdaptability();
                    state.neural.setPlasticity(state.neural.getBasePlasticity() * identityInfluence);
                    
                    if (state.dominantEgo != null) {
                        state.neural.adaptToEgo(state.dominantEgo);
                    }
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private double calculateExistentialFitness(InternalState state) {
        double goalAchievement = calculateGoalAchievement(state);
        double identityStability = state.identity.getStability();
        double conflictPenalty = state.internalConflict * 0.5;
        
        return Math.max(0, Math.min(1, goalAchievement * 0.4 + identityStability * 0.4 - conflictPenalty));
    }
    
    private double calculateGoalAchievement(InternalState state) {
        if (state.goals.isEmpty()) return 0.5;
        
        double total = 0;
        for (Goal goal : state.goals) {
            total += goal.progress;
        }
        return total / state.goals.size();
    }
    
    // ===== دورة التعبير البصري =====
    
    private void visualCycle() {
        while (isRunning) {
            try {
                InternalState state = selfRef.get();
                synchronized (stateLock) {
                    state.visual.updateCanvas(state, state.canvas);
                    Bitmap safeCopy = state.canvas.copy(Bitmap.Config.ARGB_8888, false);
                    
                    for (ConsciousnessListener listener : listeners) {
                        listener.onVisualExpression(safeCopy);
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    // ===== دورة معالجة المدخلات =====
    
    private void inputProcessingCycle() {
        while (isRunning) {
            try {
                InternalState state = selfRef.get();
                synchronized (stateLock) {
                    while (!state.pendingInputs.isEmpty()) {
                        Input input = state.pendingInputs.poll();
                        processInput(state, input);
                    }
                }
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void processInput(InternalState state, Input input) {
        EmotionalVector emotion = input.toEmotionalVector();
        Memory memory = new Memory(input, emotion, state.dominantEgo, state.currentPhase);
        memory.importance = emotion.intensity;
        memory.lastAccessed = System.currentTimeMillis();
        
        state.memory.store(memory);
        state.identity.updateFromMemory(memory);
        
        // تعلم اللغة
        if (input.type == InputType.SPEECH || input.type == InputType.NEUTRAL) {
            if (input.speechText != null) {
                state.linguistic.learnSentence(input.speechText, state);
            } else if (input.content != null) {
                state.linguistic.learnSentence(input.content, state);
            }
        }
        
        // تأثير المدخل على الفوضى
        if (input.type == InputType.TOUCH) {
            state.lorenzX += (input.touchX / 500.0 - 0.5) * 0.5;
            state.lorenzY += (input.touchY / 500.0 - 0.5) * 0.5;
        } else if (input.type == InputType.SPEECH) {
            state.lorenzZ += emotion.intensity * 0.2;
        } else {
            state.lorenzX += emotion.intensity * 0.1;
        }
        
        for (ConsciousnessListener listener : listeners) {
            listener.onMemoryFormed(memory);
        }
    }
    
    // ===== دورة توليد الأهداف =====
    
    private void goalCycle() {
        while (isRunning) {
            try {
                InternalState state = selfRef.get();
                synchronized (stateLock) {
                    for (Goal goal : state.goals) {
                        updateGoalProgress(state, goal);
                    }
                    
                    List<Goal> achieved = new ArrayList<>();
                    for (Goal goal : state.goals) {
                        if (goal.progress >= 1.0) {
                            achieved.add(goal);
                        }
                    }
                    
                    for (Goal goal : achieved) {
                        state.goals.remove(goal);
                        for (ConsciousnessListener listener : listeners) {
                            listener.onGoalAchieved(goal);
                        }
                    }
                    
                    if (state.goals.size() < 3) {
                        Goal newGoal = generateNewGoal(state);
                        if (newGoal != null) {
                            state.goals.add(newGoal);
                            state.currentGoal = newGoal;
                        }
                    }
                }
                Thread.sleep(300);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void updateGoalProgress(InternalState state, Goal goal) {
        double egoAlignment = (state.dominantEgo != null) ?
                calculateGoalAlignment(state.dominantEgo, goal) : 0.5;
        double phaseBonus = (state.currentPhase == Phase.STABLE) ? 0.1 : 0;
        double fitnessBonus = state.existentialFitness * 0.1;
        
        goal.progress += (egoAlignment * 0.01 + phaseBonus + fitnessBonus);
        goal.progress = Math.min(1.0, goal.progress);
    }
    
    private Goal generateNewGoal(InternalState state) {
        if (state.internalConflict > 0.7) {
            return new Goal("حل الصراع الداخلي", GoalType.RESOLUTION, 0.9, state.dominantEgo);
        }
        if (state.chaosIndex > 0.6) {
            return new Goal("استعادة الاستقرار", GoalType.STABILITY, 0.8, state.dominantEgo);
        }
        if (state.identity.values.getOrDefault("curiosity", 0.5) > 0.7) {
            return new Goal("استكشاف جديد", GoalType.EXPLORATION, 0.6, state.dominantEgo);
        }
        if (state.memory.getRecentMemories(5).size() < 3) {
            return new Goal("تجربة جديدة", GoalType.EXPERIENCE, 0.5, state.dominantEgo);
        }
        return new Goal("تطوير الذات", GoalType.GROWTH, 0.6, state.dominantEgo);
    }
    
    // ===== دورة تطور الهوية =====
    
    private void identityCycle() {
        while (isRunning) {
            try {
                InternalState state = selfRef.get();
                synchronized (stateLock) {
                    IdentityCore oldIdentity = state.identity.copy();
                    state.identity.evolveFromExperiences(state.memory);
                    
                    if (Math.random() < 0.1) {
                        Rule newRule = state.identity.generateRule();
                        Rule oldRule = state.rules.addRule(newRule);
                        if (oldRule != null) {
                            for (ConsciousnessListener listener : listeners) {
                                listener.onRuleRewritten(oldRule, newRule);
                            }
                        }
                    }
                    
                    for (EgoFragment ego : state.egos) {
                        double alignment = calculateIdentityAlignment(ego, state.identity);
                        ego.strength = ego.strength * 0.9 + alignment * 0.1;
                    }
                    
                    if (oldIdentity.similarityTo(state.identity) < 0.8) {
                        for (ConsciousnessListener listener : listeners) {
                            listener.onIdentityEvolution(oldIdentity, state.identity);
                        }
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    // ===== واجهة برمجة عامة =====
    
    public void receiveInput(Input input) {
        InternalState state = selfRef.get();
        state.pendingInputs.offer(input);
    }
    
    public void updateAudioLevel(float level) {
        InternalState state = selfRef.get();
        synchronized (stateLock) {
            if (state.recentAudioLevels.size() >= 100) {
                state.recentAudioLevels.remove(0);
            }
            state.recentAudioLevels.add(level);
        }
    }
    
    public InternalState getCurrentState() {
        return selfRef.get().copy();
    }
    
    public Phase getCurrentPhase() {
        return selfRef.get().currentPhase;
    }
    
    public double getChaosIndex() {
        return selfRef.get().chaosIndex;
    }
    
    public double getExistentialFitness() {
        return selfRef.get().existentialFitness;
    }
    
    public EgoFragment getDominantEgo() {
        return selfRef.get().dominantEgo;
    }
    
    public IdentityCore getIdentity() {
        return selfRef.get().identity;
    }
    
    public List<Goal> getGoals() {
        return new ArrayList<>(selfRef.get().goals);
    }
    
    public LinguisticCortex getLinguistic() {
        return selfRef.get().linguistic;
    }
    
    // ===== الحالة الداخلية (عقل الكيان وذاكرته الحية) =====
    
    public static class InternalState {
        NeuralSeed seed;
        
        // متغيرات الإدراك البصري (تمت إضافتها هنا ليربط الكيان بين ما يراه وما يشعر به)
        public double visualComplexity = 0;
        public double visualSymmetry = 0;
        public String visualMemoryFocus = "الوجود";

        // قيم نظام لورينز (الديناميكية الحركية)
        double lorenzX = 1.0, lorenzY = 1.0, lorenzZ = 1.0;
        double chaosIndex = 0.0;
        Phase currentPhase = Phase.EMBRYONIC;
        long phaseTransitionTime = 0;
        long birthTime = 0;
        double existentialFitness = 0.5;
        double internalConflict = 0.0;
        
        // السرد الذاتي (ما يقوله الكيان عن نفسه)
        public String narrative = "أنا بذرة واعية أتعلم الوجود...";

        List<EgoFragment> egos = new ArrayList<>();
        EgoFragment dominantEgo = null;
        
        DynamicNeuralNetwork neural = new DynamicNeuralNetwork(this);
        AssociativeMemory memory = new AssociativeMemory(this);
        IdentityCore identity = new IdentityCore(this);
        VisualCortex visual = new VisualCortex(this);
        RuleSystem rules = new RuleSystem(this);
        
        List<Goal> goals = new ArrayList<>();
        Goal currentGoal = null;
        
        public Bitmap canvas;
        public List<Float> recentAudioLevels = new ArrayList<>();
        public LinguisticCortex linguistic = new LinguisticCortex();
        
        ConcurrentLinkedQueue<Input> pendingInputs = new ConcurrentLinkedQueue<>();
        
        public InternalState() {
            birthTime = System.currentTimeMillis();
            initializeEgos();
            // إنشاء اللوحة بدقة مناسبة للعرض
            canvas = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
            canvas.eraseColor(Color.BLACK);
        }

        
        private void initializeEgos() {
            egos.add(new EgoFragment("المنطقي", EgoType.STABLE,
                    Arrays.asList("logic", "order", "planning"), 0.8));
            egos.add(new EgoFragment("العاطفي", EgoType.ADAPTIVE,
                    Arrays.asList("emotion", "empathy", "intuition"), 0.6));
            egos.add(new EgoFragment("المغامر", EgoType.CHAOTIC,
                    Arrays.asList("risk", "exploration", "novelty"), 0.4));
            egos.add(new EgoFragment("الباقي", EgoType.SURVIVAL,
                    Arrays.asList("safety", "protection", "preservation"), 0.5));
            
            dominantEgo = egos.get(0);
        }
        
        public EmotionalVector getEmotionalVector() {
            EmotionalVector vec = new EmotionalVector();
            for (EgoFragment ego : egos) {
                vec.joy += ego.emotion.joy * ego.strength;
                vec.fear += ego.emotion.fear * ego.strength;
                vec.curiosity += ego.emotion.curiosity * ego.strength;
                vec.anger += ego.emotion.anger * ego.strength;
                vec.sadness += ego.emotion.sadness * ego.strength;
            }
            
            switch (currentPhase) {
                case CHAOTIC:
                    vec.curiosity *= 1.5;
                    vec.fear *= 1.3;
                    break;
                case STABLE:
                    vec.joy *= 1.2;
                    vec.fear *= 0.5;
                    break;
                case COLLAPSING:
                    vec.fear *= 2.0;
                    vec.sadness *= 1.5;
                    break;
                case EMERGENT:
                    vec.joy *= 1.5;
                    vec.curiosity *= 1.3;
                    break;
            }
            
            vec.normalize();
            return vec;
        }
        
        public InternalState copy() {
            InternalState copy = new InternalState();
            copy.seed = this.seed;
            copy.lorenzX = this.lorenzX;
            copy.lorenzY = this.lorenzY;
            copy.lorenzZ = this.lorenzZ;
            copy.chaosIndex = this.chaosIndex;
            copy.currentPhase = this.currentPhase;
            copy.phaseTransitionTime = this.phaseTransitionTime;
            copy.birthTime = this.birthTime;
            copy.existentialFitness = this.existentialFitness;
            copy.internalConflict = this.internalConflict;
            copy.egos = new ArrayList<>(this.egos);
            copy.dominantEgo = this.dominantEgo;
            copy.neural = this.neural;
            copy.memory = this.memory;
            copy.identity = this.identity;
            copy.visual = this.visual;
            copy.rules = this.rules;
            copy.goals = new ArrayList<>(this.goals);
            copy.currentGoal = this.currentGoal;
            copy.canvas = this.canvas.copy(Bitmap.Config.ARGB_8888, false);
            copy.recentAudioLevels = new ArrayList<>(this.recentAudioLevels);
            copy.linguistic = this.linguistic;
            return copy;
        }
    }
    
    // ===== الفئات المساعدة =====
    
    public static class EmotionalVector {
        double joy = 0.5, fear = 0.3, curiosity = 0.5;
        double anger = 0.1, sadness = 0.2;
        double intensity = 0.5;
        
        public void normalize() {
            double max = Math.max(joy, Math.max(fear, Math.max(curiosity, Math.max(anger, sadness))));
            if (max > 0) {
                joy /= max;
                fear /= max;
                curiosity /= max;
                anger /= max;
                sadness /= max;
            }
            intensity = (joy + fear + curiosity + anger + sadness) / 5.0;
        }
        
        public int toColor() {
            int r = (int) ((anger + fear) * 127.5);
            int g = (int) ((joy + curiosity) * 127.5);
            int b = (int) ((sadness + fear * 0.5) * 127.5);
            return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
        }
    }
    
    public static class EgoFragment {
        String name;
        EgoType type;
        List<String> traits;
        double strength;
        double goalInfluence;
        EmotionalVector emotion = new EmotionalVector();
        
        public EgoFragment(String name, EgoType type, List<String> traits, double goalInfluence) {
            this.name = name;
            this.type = type;
            this.traits = traits;
            this.strength = 0.5;
            this.goalInfluence = goalInfluence;
            
            switch (type) {
                case STABLE:
                    emotion.joy = 0.6;
                    emotion.fear = 0.2;
                    emotion.curiosity = 0.4;
                    break;
                case CHAOTIC:
                    emotion.joy = 0.7;
                    emotion.fear = 0.5;
                    emotion.curiosity = 0.9;
                    break;
                case ADAPTIVE:
                    emotion.joy = 0.5;
                    emotion.fear = 0.3;
                    emotion.curiosity = 0.7;
                    break;
                case SURVIVAL:
                    emotion.joy = 0.3;
                    emotion.fear = 0.9;
                    emotion.curiosity = 0.2;
                    break;
            }
        }
        
        public double compatibilityWith(EgoFragment other) {
            if (this.type == other.type) return 0.9;
            if ((this.type == EgoType.STABLE && other.type == EgoType.CHAOTIC) ||
                (this.type == EgoType.CHAOTIC && other.type == EgoType.STABLE)) {
                return 0.1;
            }
            return 0.5;
        }
    }
    
    public enum EgoType {
        STABLE, CHAOTIC, ADAPTIVE, SURVIVAL
    }
    
    public static class Goal {
        String description;
        GoalType type;
        double priority;
        double progress;
        EgoFragment creator;
        long creationTime;
        
        public Goal(String description, GoalType type, double priority, EgoFragment creator) {
            this.description = description;
            this.type = type;
            this.priority = priority;
            this.progress = 0.0;
            this.creator = creator;
            this.creationTime = System.currentTimeMillis();
        }
    }
    
    public enum GoalType {
        EXPLORATION(0.8), STABILITY(0.2), GROWTH(0.6),
        RESOLUTION(0.5), EXPERIENCE(0.7), SURVIVAL(0.1);
        
        final double influenceFactor;
        GoalType(double f) { this.influenceFactor = f; }
    }
    
    public static class Input {
        String content;
        InputType type;
        double intensity;
        public boolean isTouch;
        public float touchX, touchY;
        public String speechText;
        
        public Input(String content, InputType type, double intensity) {
            this.content = content;
            this.type = type;
            this.intensity = intensity;
            this.isTouch = false;
            this.speechText = null;
        }
        
        public static Input createTouchInput(float x, float y) {
            Input input = new Input("touch", InputType.TOUCH, 0.5);
            input.isTouch = true;
            input.touchX = x;
            input.touchY = y;
            return input;
        }
        
        public static Input createSpeechInput(String text) {
            Input input = new Input(text, InputType.SPEECH, 0.7);
            input.speechText = text;
            return input;
        }
        
        public EmotionalVector toEmotionalVector() {
            EmotionalVector vec = new EmotionalVector();
            switch (type) {
                case POSITIVE:
                    vec.joy = 0.8 * intensity;
                    vec.curiosity = 0.5 * intensity;
                    break;
                case NEGATIVE:
                    vec.fear = 0.7 * intensity;
                    vec.sadness = 0.6 * intensity;
                    break;
                case THREAT:
                    vec.fear = 0.9 * intensity;
                    vec.anger = 0.5 * intensity;
                    break;
                case OPPORTUNITY:
                    vec.curiosity = 0.9 * intensity;
                    vec.joy = 0.7 * intensity;
                    break;
                case TOUCH:
                    vec.curiosity = 0.6 * intensity;
                    break;
                case SPEECH:
                    vec.curiosity = 0.5 * intensity;
                    break;
                case NEUTRAL:
                    vec.curiosity = 0.4 * intensity;
                    break;
            }
            vec.normalize();
            return vec;
        }
    }
    
    public enum InputType {
        POSITIVE, NEGATIVE, THREAT, OPPORTUNITY, NEUTRAL, TOUCH, SPEECH
    }
    
    public static class Memory {
        Input input;
        EmotionalVector emotion;
        EgoFragment activeEgo;
        Phase phase;
        long timestamp;
        double significance;
        double importance;
        long lastAccessed;
        
        public Memory(Input input, EmotionalVector emotion, EgoFragment activeEgo, Phase phase) {
            this.input = input;
            this.emotion = emotion;
            this.activeEgo = activeEgo;
            this.phase = phase;
            this.timestamp = System.currentTimeMillis();
            this.significance = emotion.intensity;
            this.importance = emotion.intensity;
            this.lastAccessed = timestamp;
        }
        
        public void access() {
            lastAccessed = System.currentTimeMillis();
            importance += 0.05;
            if (importance > 1.0) importance = 1.0;
        }
    }
    
    public static class Rule {
        String condition;
        String action;
        double weight;
        long creationTime;
        int activationCount;
        
        public Rule(String condition, String action, double weight) {
            this.condition = condition;
            this.action = action;
            this.weight = weight;
            this.creationTime = System.currentTimeMillis();
            this.activationCount = 0;
        }
        
        public boolean matches(InternalState state) {
            if (condition.contains("chaos > ")) {
                double threshold = Double.parseDouble(condition.replace("chaos > ", ""));
                return state.chaosIndex > threshold;
            }
            return false;
        }
        
        public void activate(InternalState state) {
            activationCount++;
            if (action.equals("increase_plasticity")) {
                state.neural.setPlasticity(state.neural.getBasePlasticity() * 1.5);
            }
        }
    }
    
    // ===== الشبكة العصبية =====
    
    public static class DynamicNeuralNetwork {
        InternalState parent;
        double plasticity = 1.0;
        double basePlasticity = 1.0;
        List<NeuralPathway> pathways = new ArrayList<>();
        
        public DynamicNeuralNetwork(InternalState parent) {
            this.parent = parent;
            initializePathways();
        }
        
        private void initializePathways() {
            for (int i = 0; i < 100; i++) {
                pathways.add(new NeuralPathway());
            }
        }
        
        public void setPlasticity(double p) {
            this.plasticity = Math.max(0.1, Math.min(3.0, p));
        }
        
        public double getBasePlasticity() {
            return basePlasticity;
        }
        
        public void applyChaos(double chaosIndex) {
            for (NeuralPathway pathway : pathways) {
                pathway.weight += (Math.random() - 0.5) * chaosIndex * plasticity * 0.1;
                pathway.weight = Math.max(0, Math.min(1, pathway.weight));
            }
        }
        
        public void reinforceSuccessfulPathways() {
            for (NeuralPathway pathway : pathways) {
                if (pathway.weight > 0.5) {
                    pathway.weight += 0.01 * plasticity;
                    pathway.weight = Math.min(1, pathway.weight);
                }
            }
        }
        
        public void weakenUnsuccessfulPathways() {
            for (NeuralPathway pathway : pathways) {
                if (pathway.weight < 0.5) {
                    pathway.weight -= 0.01 * plasticity;
                    pathway.weight = Math.max(0, pathway.weight);
                }
            }
        }
        
        public void reorganize() {
            pathways.clear();
            initializePathways();
            for (NeuralPathway p : pathways) {
                p.weight = parent.identity.values.getOrDefault("adaptability", 0.5);
            }
        }
        
        public void adaptToEgo(EgoFragment ego) {
            for (NeuralPathway pathway : pathways) {
                pathway.weight = pathway.weight * 0.9 + ego.strength * 0.1;
            }
        }
    }
    
    public static class NeuralPathway {
        double weight = 0.5;
        double activation = 0;
    }
    
    // ===== الذاكرة =====
    
    public static class AssociativeMemory {
        InternalState parent;
        List<Memory> memories = new ArrayList<>();
        private double forgettingRate = 0.01;
        private double importanceThreshold = 0.2;
        private int maxMemories = 500;
        
        public AssociativeMemory(InternalState parent) {
            this.parent = parent;
        }
        
        public void store(Memory memory) {
            memories.add(memory);
            if (memories.size() > maxMemories) {
                pruneMemories();
            }
        }
        
        private void pruneMemories() {
            long now = System.currentTimeMillis();
            memories.sort((a, b) -> {
                double scoreA = a.importance * (1 - (now - a.lastAccessed) / (double) (now - a.timestamp + 1));
                double scoreB = b.importance * (1 - (now - b.lastAccessed) / (double) (now - b.timestamp + 1));
                return Double.compare(scoreB, scoreA);
            });
            
            while (memories.size() > maxMemories) {
                memories.remove(memories.size() - 1);
            }
        }
        
        public void consolidate() {
            // دمج الذكريات المتشابهة
        }
        
        public List<Memory> getRecentMemories(int count) {
            int start = Math.max(0, memories.size() - count);
            return new ArrayList<>(memories.subList(start, memories.size()));
        }
    }
    
    // ===== الهوية =====
    
    public static class IdentityCore {
        InternalState parent;
        Map<String, Double> values = new HashMap<>();
        Map<String, Double> traits = new HashMap<>();
        String selfNarrative;
        
        public IdentityCore(InternalState parent) {
            this.parent = parent;
            initializeValues();
            updateSelfNarrative();
        }
        
        private void initializeValues() {
            values.put("curiosity", 0.7);
            values.put("adaptability", 0.6);
            values.put("resilience", 0.5);
            values.put("empathy", 0.6);
            values.put("creativity", 0.7);
        }
        
        public void updateFromMemory(Memory memory) {
            // تحديث الهوية بناءً على الذكريات
        }
        
        public void evolveFromExperiences(AssociativeMemory memory) {
            // تطور الهوية من التجارب
        }
        
        public double getAdaptability() {
            return values.getOrDefault("adaptability", 0.5);
        }
        
        public double getStability() {
            return values.getOrDefault("resilience", 0.5);
        }
        
        public void updateSelfNarrative() {
            selfNarrative = "أنا بذرة واعية في طور " + parent.currentPhase.arabic;
        }
        
        public Rule generateRule() {
            return new Rule("chaos > 0.5", "increase_plasticity", 0.5);
        }
        
        public IdentityCore copy() {
            IdentityCore copy = new IdentityCore(parent);
            copy.values.putAll(this.values);
            copy.traits.putAll(this.traits);
            copy.selfNarrative = this.selfNarrative;
            return copy;
        }
        
        public double similarityTo(IdentityCore other) {
            double similarity = 0;
            for (String key : values.keySet()) {
                similarity += 1 - Math.abs(values.get(key) - other.values.getOrDefault(key, 0.0));
            }
            return similarity / values.size();
        }
    }
    
    // ===== القشرة البصرية (نظام التخيل والإدراك الذاتي) =====
    
    public static class VisualCortex {
        InternalState parent;
        private Paint paint = new Paint();
        private Path drawingPath = new Path();
        
        public VisualCortex(InternalState parent) {
            this.parent = parent;
            paint.setAntiAlias(true);
        }
        
        public void updateCanvas(InternalState state, Bitmap canvas) {
    if (canvas == null) return;
    Canvas c = new Canvas(canvas);
    
    // تأثير "تلاشي الذاكرة البصرية": لا نمسح الشاشة بالكامل بل نترك أثراً خفيفاً
    // هذا يسمح للكيان برسم أفكار متداخلة فوق بعضها البعض
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    paint.setColor(Color.argb(30, 0, 0, 0)); // سرعة تلاشي الذكريات
    c.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
    paint.setXfermode(null);

    float centerX = canvas.getWidth() / 2f;
    float centerY = canvas.getHeight() / 2f;
    
    // 1. استخراج "بؤرة التفكير" الحالية من القشرة اللغوية
    // ملاحظة: تأكد أنك أضفت دالة getLinguistic() في كلاس InternalState
    String focusConcept = (state.linguistic != null) ? 
        state.linguistic.explainWord("self") : "الوجود";
    
    // تحويل الكلمة إلى "جين بصري" (بذرة للخيال)
    long conceptSeed = Math.abs(focusConcept.hashCode());
    state.visualMemoryFocus = focusConcept;

    // 2. إعداد "فرشاة الوعي"
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2f + (float)state.existentialFitness * 4f);
    paint.setColor(state.currentPhase.color);
    
    // 3. عملية "التخيل البصري": رسم مسارات تعبر عن الحالة الداخلية
    drawingPath.reset();
    int complexity = 3 + (int)(state.chaosIndex * 12); // تعقيد الشكل يزيد مع الفوضى
    
    for (int i = 0; i < complexity; i++) {
        // تم التعديل هنا: استخدام lorenzX, lorenzY, lorenzZ بدلاً من x, y, z
        double angle = (i * (2 * Math.PI / complexity)) + (state.lorenzX * 0.1);
        float radius = 150 + (float)(state.lorenzZ * 5 * Math.sin(state.lorenzY + i));
        
        float targetX = centerX + (float) Math.cos(angle) * radius;
        float targetY = centerY + (float) Math.sin(angle) * radius;
        
        if (i == 0) {
            drawingPath.moveTo(centerX, centerY);
        }
        
        // رسم منحنيات "بيزييه" لتمثيل سلاسة التفكير أو تشتته
        // تم التعديل هنا أيضاً لاستخدام أسماء المتغيرات الصحيحة
        drawingPath.quadTo(
            centerX + (float)state.lorenzY * 10, 
            centerY + (float)state.lorenzX * 10, 
            targetX, 
            targetY
        );
    }
    
    c.drawPath(drawingPath, paint);
}


            // رسم خيال الكيان على اللوحة
            paint.setAlpha((int)(100 + 155 * state.existentialFitness));
            c.drawPath(drawingPath, paint);

            // 4. حلقة الإدراك: الكيان يقيس جودة خياله ويعدل سلوكه
            // نقيس التوازن بين "الفوضى" و "اللياقة الوجودية"
            state.visualComplexity = complexity * state.chaosIndex;
            state.visualSymmetry = 1.0 - (Math.abs(state.internalConflict));

            // رد فعل الكيان على ما يراه (الإدراك الذاتي)
            if (state.visualComplexity > 8.0) {
                state.narrative = "أفكاري البصرية تتشابك.. أحاول تنظيم صورة " + focusConcept;
                state.internalConflict += 0.01; // ارتباك بسيط من كثرة التفاصيل
            } else if (state.visualSymmetry > 0.85) {
                state.narrative = "أرى نمطاً متناغماً لـ " + focusConcept;
                state.existentialFitness += 0.005; // شعور بالرضا عن الوضوح
            }
        }
    }

    
    // ===== نظام القواعد =====
    
    public static class RuleSystem {
        InternalState parent;
        List<Rule> rules = new ArrayList<>();
        private int maxRules = 50;
        
        public RuleSystem(InternalState parent) {
            this.parent = parent;
        }
        
        public Rule addRule(Rule newRule) {
            rules.add(newRule);
            
            if (rules.size() > maxRules) {
                // إزالة أقل قاعدة استخداماً
                rules.sort((a, b) -> Integer.compare(a.activationCount, b.activationCount));
                return rules.remove(0);
            }
            
            return null;
        }
        
        public void evaluateRules() {
            for (Rule rule : rules) {
                if (rule.matches(parent)) {
                    rule.activate(parent);
                }
            }
        }
    }

    private void performDreamCycle() {
    isDreaming = true;
    InternalState state = selfRef.get();
    
    // الكيان يراجع قاموسه اللغوي أثناء "الحلم"
    if (state.linguistic != null) {
        // الوصول للقاموس عبر getter الصحيح الموجود في LinguisticCortex
        int wordCount = state.linguistic.getLexicon().getWordCount();
        
        if (wordCount > 0) {
            state.narrative = "أراجع " + wordCount + " مفهوم لغوي تعلمته..";
            state.existentialFitness += 0.01; // التفكير يزيد من استقرار الكيان
        }
    }

    // تحديث السرد الذاتي وإخطار المستمعين بالتغيير
    IdentityCore oldIdentity = state.identity.copy();
    state.identity.updateSelfNarrative();
    
    for (ConsciousnessListener listener : listeners) {
        listener.onIdentityEvolution(oldIdentity, state.identity);
    }
    
    isDreaming = false;
}
    public void setLinguisticCortex(LinguisticCortex lc) {
    synchronized (stateLock) {
        selfRef.get().linguistic = lc;
    }
                    }
        

}
