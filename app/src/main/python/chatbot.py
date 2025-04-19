import json
import random
import os
from pathlib import Path
import sys
import difflib

# Logging function
def log_error(message):
    with open('chatbot_error.log', 'a') as f:
        f.write(f"{message}\n")

# Try importing NLTK (optional for better NLP)
try:
    import nltk
    from nltk.tokenize import word_tokenize
    from nltk.corpus import stopwords
    from nltk.stem import WordNetLemmatizer
    nltk.download('punkt', quiet=True)
    nltk.download('wordnet', quiet=True)
    nltk.download('stopwords', quiet=True)
except Exception as e:
    log_error(f"Failed to import NLTK: {str(e)}")
    def word_tokenize(text):
        return text.lower().split()

class ChatBot:
    def __init__(self):
        try:
            self.fallback_responses = [
                "I'm here! How can I help?",
                "Hello there!",
                "Hi! What can I do for you?"
            ]

            # Load intents file
            intents_path = Path(__file__).parent / 'intent.json'
            try:
                with open(intents_path, 'r') as file:
                    self.intents = json.load(file)
            except Exception as e:
                log_error(f"Failed to load intent.json: {str(e)}")
                self.intents = {"intents": [{"tag": "fallback", "patterns": [""], "responses": self.fallback_responses}]}

            try:
                self.lemmatizer = WordNetLemmatizer()
            except Exception as e:
                log_error(f"Failed to initialize lemmatizer: {str(e)}")
                self.lemmatizer = None

        except Exception as e:
            log_error(f"ChatBot initialization error: {str(e)}")

    def preprocess_text(self, text):
        """ Tokenizes, removes stopwords, and lemmatizes text (if NLTK is available) """
        try:
            words = word_tokenize(text.lower().strip())
            words = [self.lemmatizer.lemmatize(w) for w in words if w.isalnum()] if self.lemmatizer else words
            return " ".join(words)
        except Exception as e:
            log_error(f"Preprocessing error: {str(e)}")
            return text.lower().strip()

    def get_best_match(self, user_input, patterns):
        """ Finds the best matching pattern using difflib """
        best_match = difflib.get_close_matches(user_input, patterns, n=1, cutoff=0.5)  # 50% similarity threshold
        return best_match[0] if best_match else None

    def get_response(self, user_input):
        """ Returns chatbot response based on fuzzy matching """
        try:
            if not user_input or not user_input.strip():
                return random.choice(self.fallback_responses)

            processed_input = self.preprocess_text(user_input)

            for intent in self.intents['intents']:
                patterns = [self.preprocess_text(p) for p in intent['patterns']]
                match = self.get_best_match(processed_input, patterns)

                if match:
                    return random.choice(intent['responses'])

            return random.choice(self.fallback_responses)

        except Exception as e:
            log_error(f"Error getting response: {str(e)}")
            return "I'm here! How can I help?"

# Initialize chatbot globally
try:
    chatbot = ChatBot()
except Exception as e:
    log_error(f"Failed to create ChatBot instance: {str(e)}")
    chatbot = None

def get_response(message):
    """ Global function for Java to call """
    try:
        if chatbot is None:
            return "Chat system is initializing. Please try again."
        return chatbot.get_response(message)
    except Exception as e:
        log_error(f"Error in get_response: {str(e)}")
        return "I'm here! How can I help?"
