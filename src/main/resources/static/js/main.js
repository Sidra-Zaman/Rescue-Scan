document.addEventListener('DOMContentLoaded', function() {
    const forms = document.querySelectorAll('form');

    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const buttons = form.querySelectorAll('button[type="submit"]');
            buttons.forEach(btn => {
                btn.disabled = true;
                btn.textContent = 'Processing...';
            });
        });
    });

    const phoneInputs = document.querySelectorAll('input[type="tel"]');
    phoneInputs.forEach(input => {
        input.addEventListener('input', function(e) {
            this.value = this.value.replace(/[^\d+\-() ]/g, '');
        });
    });
});
