package com.example.data.model

enum class QuoteCategory(val label: String, val emoji: String, val hexColor: String) {
    TIME("Time & Focus", "⏳", "#3B82F6"),
    DISCIPLINE("Discipline", "🛡️", "#8B5CF6"),
    MOTIVATION("Motivation", "🔥", "#EF4444"),
    SUCCESS("Success", "🏆", "#F59E0B"),
    CONSISTENCY("Consistency", "⚡", "#00E5BE")
}

enum class QuoteTriggerReason(val dialogTitle: String, val subtitle: String) {
    APP_LAUNCH("Daily Wisdom ✨", "Start your session with focus and clarity"),
    TASK_CREATED("Task Created 🎯", "Great job planning ahead! Keep the momentum going"),
    NOTE_CREATED("Note Saved 💡", "Ideas turn into reality with steady action"),
    HABIT_CREATED("Habit Created 🔥", "Small daily habits build an extraordinary life"),
    ROUTINE_CREATED("Routine Scheduled ⏰", "Consistency in daily routines shapes an extraordinary life")
}

data class QuoteItem(
    val id: Int,
    val text: String,
    val author: String,
    val category: QuoteCategory
)

data class QuoteDialogState(
    val quote: QuoteItem,
    val triggerReason: QuoteTriggerReason
)

object QuotesCatalog {
    val QUOTES: List<QuoteItem> = listOf(
        // TIME QUOTES (Short & Fit-to-Box)
        QuoteItem(1, "Time is what we want most, but use worst.", "William Penn", QuoteCategory.TIME),
        QuoteItem(2, "Lost time is never found again.", "Benjamin Franklin", QuoteCategory.TIME),
        QuoteItem(3, "Time flies, but you are the pilot.", "Michael Altshuler", QuoteCategory.TIME),
        QuoteItem(4, "Time is more valuable than money.", "Jim Rohn", QuoteCategory.TIME),
        QuoteItem(5, "You don't need more time, just decide.", "Seth Godin", QuoteCategory.TIME),
        QuoteItem(6, "Do it today; tomorrow never comes.", "Sant Kabir", QuoteCategory.TIME),
        QuoteItem(7, "Yesterday is gone. Today is ours. Begin.", "Mother Teresa", QuoteCategory.TIME),
        QuoteItem(8, "Schedule your priorities, not tasks.", "Stephen Covey", QuoteCategory.TIME),
        QuoteItem(9, "Never waste a single minute of your day.", "Chanakya", QuoteCategory.TIME),
        QuoteItem(10, "Value time, and time will value your work.", "Sardar Vallabhbhai Patel", QuoteCategory.TIME),

        // DISCIPLINE QUOTES
        QuoteItem(11, "Discipline is choosing what you want most.", "Abraham Lincoln", QuoteCategory.DISCIPLINE),
        QuoteItem(12, "Choose the pain of discipline over regret.", "Jim Rohn", QuoteCategory.DISCIPLINE),
        QuoteItem(13, "Master your thoughts to master your life.", "Napoleon Hill", QuoteCategory.DISCIPLINE),
        QuoteItem(14, "Discipline bridges goals and results.", "Jim Rohn", QuoteCategory.DISCIPLINE),
        QuoteItem(15, "Mastering yourself is true inner power.", "Lao Tzu", QuoteCategory.DISCIPLINE),
        QuoteItem(16, "Rule your mind, or it will rule you.", "Horace", QuoteCategory.DISCIPLINE),
        QuoteItem(17, "Small daily disciplines build great results.", "John C. Maxwell", QuoteCategory.DISCIPLINE),
        QuoteItem(18, "Discipline is the true path to freedom.", "Jocko Willink", QuoteCategory.DISCIPLINE),
        QuoteItem(19, "Discipline turns ordinary days into wins.", "Milkha Singh", QuoteCategory.DISCIPLINE),
        QuoteItem(20, "Be sincere in work; duty is true worship.", "Sardar Vallabhbhai Patel", QuoteCategory.DISCIPLINE),
        QuoteItem(21, "Arise, awake, stop not till goal is reached.", "Swami Vivekananda", QuoteCategory.DISCIPLINE),

        // MOTIVATION QUOTES
        QuoteItem(22, "The secret of getting ahead is starting.", "Mark Twain", QuoteCategory.MOTIVATION),
        QuoteItem(23, "Believe you can and you're halfway there.", "Theodore Roosevelt", QuoteCategory.MOTIVATION),
        QuoteItem(24, "Don't watch the clock; keep moving forward.", "Sam Levenson", QuoteCategory.MOTIVATION),
        QuoteItem(25, "Act as if what you do makes a difference.", "William James", QuoteCategory.MOTIVATION),
        QuoteItem(26, "Start where you are. Use what you have.", "Arthur Ashe", QuoteCategory.MOTIVATION),
        QuoteItem(27, "Your future is shaped by what you do today.", "Robert Kiyosaki", QuoteCategory.MOTIVATION),
        QuoteItem(28, "Energy flows where your attention goes.", "Tony Robbins", QuoteCategory.MOTIVATION),
        QuoteItem(29, "You must start in order to become great.", "Zig Ziglar", QuoteCategory.MOTIVATION),
        QuoteItem(30, "Dream big before dreams can come true.", "Dr. A.P.J. Abdul Kalam", QuoteCategory.MOTIVATION),
        QuoteItem(31, "Be the change you wish to see in the world.", "Mahatma Gandhi", QuoteCategory.MOTIVATION),
        QuoteItem(32, "Faith is the bird that sings before dawn.", "Rabindranath Tagore", QuoteCategory.MOTIVATION),
        QuoteItem(33, "If you don't build your dream, others will.", "Dhirubhai Ambani", QuoteCategory.MOTIVATION),
        QuoteItem(34, "Take risks: win to lead, lose to guide.", "Swami Vivekananda", QuoteCategory.MOTIVATION),
        QuoteItem(35, "Strength comes from an indomitable will.", "Mahatma Gandhi", QuoteCategory.MOTIVATION),

        // SUCCESS QUOTES
        QuoteItem(36, "Success is small efforts repeated daily.", "Robert Collier", QuoteCategory.SUCCESS),
        QuoteItem(37, "Courage to continue is what truly counts.", "Winston Churchill", QuoteCategory.SUCCESS),
        QuoteItem(38, "Success comes before work only in dictionaries.", "Vidal Sassoon", QuoteCategory.SUCCESS),
        QuoteItem(39, "Stay busy building, and success will follow.", "Henry David Thoreau", QuoteCategory.SUCCESS),
        QuoteItem(40, "Action is the true key to all success.", "Pablo Picasso", QuoteCategory.SUCCESS),
        QuoteItem(41, "Constancy to purpose is key to success.", "Benjamin Disraeli", QuoteCategory.SUCCESS),
        QuoteItem(42, "Hard work and consistency bring success.", "Dwayne Johnson", QuoteCategory.SUCCESS),
        QuoteItem(43, "Opportunities don't happen; you create them.", "Chris Grosser", QuoteCategory.SUCCESS),
        QuoteItem(44, "Turn stones thrown at you into milestones.", "Sachin Tendulkar", QuoteCategory.SUCCESS),
        QuoteItem(45, "Build monuments from stones thrown at you.", "Ratan Tata", QuoteCategory.SUCCESS),
        QuoteItem(46, "Excellence is a habit, not an accident.", "Dr. A.P.J. Abdul Kalam", QuoteCategory.SUCCESS),
        QuoteItem(47, "Think big, think fast, think ahead always.", "Dhirubhai Ambani", QuoteCategory.SUCCESS),
        QuoteItem(48, "Dream is what keeps you awake to achieve.", "Dr. A.P.J. Abdul Kalam", QuoteCategory.SUCCESS),

        // CONSISTENCY QUOTES
        QuoteItem(49, "Consistency transforms average into great.", "Tony Robbins", QuoteCategory.CONSISTENCY),
        QuoteItem(50, "What we do consistently shapes our lives.", "Tony Robbins", QuoteCategory.CONSISTENCY),
        QuoteItem(51, "Small daily steps lead to big results.", "Robin Sharma", QuoteCategory.CONSISTENCY),
        QuoteItem(52, "Success comes from consistent daily effort.", "Marie Forleo", QuoteCategory.CONSISTENCY),
        QuoteItem(53, "We are what we repeatedly choose to do.", "Will Durant", QuoteCategory.CONSISTENCY),
        QuoteItem(54, "Persistence cuts through rock, not power.", "Jim Watkins", QuoteCategory.CONSISTENCY),
        QuoteItem(55, "Day by day, habits define who you become.", "Heraclitus", QuoteCategory.CONSISTENCY),
        QuoteItem(56, "Long-term consistency beats short bursts.", "Bruce Lee", QuoteCategory.CONSISTENCY),
        QuoteItem(57, "Drop by drop fills the pot; stay steady.", "Gautam Buddha", QuoteCategory.CONSISTENCY),
        QuoteItem(58, "Don't stop when tired; stop when done.", "Mary Kom", QuoteCategory.CONSISTENCY),
        QuoteItem(59, "Hard work beats talent with daily effort.", "Rahul Dravid", QuoteCategory.CONSISTENCY),
        QuoteItem(60, "Steady effort makes even hard tasks easy.", "Thiruvalluvar", QuoteCategory.CONSISTENCY),
        QuoteItem(61, "You can't cross the sea by just staring.", "Rabindranath Tagore", QuoteCategory.CONSISTENCY)
    )

    fun getQuoteById(id: Int?): QuoteItem {
        if (id == null) return QUOTES.first()
        return QUOTES.firstOrNull { it.id == id } ?: QUOTES.first()
    }

    fun getNextQuote(currentId: Int?): QuoteItem {
        if (QUOTES.isEmpty()) error("Quotes catalog cannot be empty")
        if (currentId == null) return QUOTES.first()
        val currentIndex = QUOTES.indexOfFirst { it.id == currentId }
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % QUOTES.size
        return QUOTES[nextIndex]
    }

    fun getRandomQuote(category: QuoteCategory? = null, excludeId: Int? = null): QuoteItem {
        val pool = if (category != null) {
            QUOTES.filter { it.category == category }
        } else {
            QUOTES
        }.filter { it.id != excludeId }

        val candidatePool = if (pool.isNotEmpty()) pool else QUOTES
        return candidatePool.random()
    }
}
