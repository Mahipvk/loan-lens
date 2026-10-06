"""Generates SYNTHETIC Loan Estimate-style PDFs (layout loosely modeled on the CFPB Loan Estimate)
plus the ground-truth JSON used by the eval harness. All names, numbers and lenders are fictional."""
import json, os, zlib
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "samples")

def pi(amount, rate, years):
    r = rate / 100 / 12; n = years * 12
    return round(amount * r / (1 - (1 + r) ** -n), 2)

SAMPLES = [
    dict(id="sunrise-bank", lender="Sunrise Community Bank", product="30 Year Fixed Rate", loanType="Fixed Rate",
         years=30, amount=320000, rate=6.250, apr=6.312, locked=True, prepay=False, balloon=False,
         points_pct=0.0, a_fees=[("Application Fee", 400), ("Underwriting Fee", 1200)],
         b_total=1180, c_total=2350, e_total=3620, down=80000, typo=0,
         applicant="Jordan A. Rivera", email="jordan.rivera@example.com", phone="(703) 555-0142",
         flags=[]),
    dict(id="harbor-lending", lender="Harbor Point Lending LLC", product="30 Year Fixed Rate", loanType="Fixed Rate",
         years=30, amount=320000, rate=6.125, apr=6.398, locked=False, prepay=True, balloon=False,
         points_pct=1.5, a_fees=[("Processing Fee", 895), ("Underwriting Fee", 1295), ("Document Preparation Fee", 450)],
         b_total=1240, c_total=2610, e_total=3620, down=80000, typo=0,
         applicant="Jordan A. Rivera", email="jordan.rivera@example.com", phone="(703) 555-0142",
         prepay_text="As high as $6,400 if you pay off the loan during the first 3 years",
         flags=["PREPAYMENT_PENALTY", "RATE_NOT_LOCKED", "HIGH_ORIGINATION", "POINTS_PAID"]),
    dict(id="quickfund-arm", lender="QuickFund Mortgage Co.", product="5/1 Adjustable Rate", loanType="Adjustable Rate",
         years=30, amount=280000, rate=5.750, apr=7.124, locked=True, prepay=False, balloon=False,
         points_pct=0.0, a_fees=[("Origination Fee", 4200), ("Processing Fee", 995), ("Administration Fee", 405)],
         b_total=1310, c_total=2480, e_total=3100, down=35000, typo=0,
         applicant="Priya S. Natarajan", email="priya.n@example.org", phone="(571) 555-0187",
         flags=["ADJUSTABLE_RATE", "HIGH_ORIGINATION", "APR_GAP"]),
    dict(id="maple-cu", lender="Maple Valley Credit Union", product="15 Year Fixed Rate", loanType="Fixed Rate",
         years=15, amount=250000, rate=5.500, apr=5.583, locked=True, prepay=False, balloon=False,
         points_pct=0.0, a_fees=[("Application Fee", 250), ("Underwriting Fee", 500)],
         b_total=1050, c_total=1980, e_total=2900, down=62500, typo=40,
         applicant="Samuel K. Osei", email="s.osei@example.net", phone="(202) 555-0119",
         flags=["PAYMENT_MISMATCH"]),
]

def money(x): return "${:,.0f}".format(x) if float(x).is_integer() else "${:,.2f}".format(x)

def build(s):
    true_pi = pi(s["amount"], s["rate"], s["years"])
    printed_pi = round(true_pi + s["typo"], 2)       # maple-cu has a deliberate printed error
    points_amt = round(s["amount"] * s["points_pct"] / 100, 2)
    a_total = round(points_amt + sum(f for _, f in s["a_fees"]), 2)
    closing = round(a_total + s["b_total"] + s["c_total"] + s["e_total"], 2)
    cash = round(closing + s["down"], 2)

    c = canvas.Canvas(os.path.join(OUT, s["id"] + ".pdf"), pagesize=letter)
    W, H = letter
    y = H - 50
    def line(text, size=10, bold=False, dy=15, x=50):
        nonlocal y
        c.setFont("Helvetica-Bold" if bold else "Helvetica", size); c.drawString(x, y, text); y -= dy
    line(s["lender"], 13, True, 18)
    line("Loan Estimate", 18, True, 22)
    line("Save this Loan Estimate to compare with your Closing Disclosure.", 9, dy=18)
    line("DATE ISSUED  09/22/2026", 9)
    line(f"APPLICANTS  {s['applicant']}", 9)
    line(f"CONTACT  {s['email']}   {s['phone']}", 9)
    line("PROPERTY  1200 Example Lane, Springfield, VA 22150", 9)
    line(f"SALE PRICE  {money(s['amount'] + s['down'])}", 9)
    line(f"LOAN TERM  {s['years']} years      PURPOSE  Purchase      PRODUCT  {s['product']}", 9)
    line(f"LOAN TYPE  Conventional      LOAN ID #  {zlib.crc32(s["id"].encode()) % 10**10:010d}", 9)
    line(f"RATE LOCK  {'YES, until 10/22/2026 at 5:00 p.m. EDT' if s['locked'] else 'NO. Your interest rate and points can change.'}", 9, dy=24)

    line("Loan Terms", 13, True, 18)
    line(f"Loan Amount                                  {money(s['amount'])}", 10)
    line(f"Interest Rate                                {s['rate']:.3f} %" + ("   (can increase after year 5)" if s['loanType'].startswith('Adj') else ""), 10)
    line(f"Monthly Principal & Interest                 {money(printed_pi)}", 10)
    line(f"Prepayment Penalty                           {'YES  - ' + s['prepay_text'] if s['prepay'] else 'NO'}", 10)
    line(f"Balloon Payment                              {'YES' if s['balloon'] else 'NO'}", 10, dy=24)

    line("Costs at Closing", 13, True, 18)
    line(f"Estimated Closing Costs                      {money(closing)}", 10)
    line(f"Estimated Cash to Close                      {money(cash)}", 10, dy=24)

    line("Closing Cost Details", 13, True, 18)
    line(f"A. Origination Charges                       {money(a_total)}", 10, True)
    line(f"   {s['points_pct']:.2f} % of Loan Amount (Points)            {money(points_amt)}", 9)
    for n, f in s["a_fees"]:
        line(f"   {n:<38}{money(f)}", 9)
    line(f"B. Services You Cannot Shop For              {money(s['b_total'])}", 10, True)
    line(f"C. Services You Can Shop For                 {money(s['c_total'])}", 10, True)
    line(f"E. Taxes and Other Government Fees           {money(s['e_total'])}", 10, True, dy=24)

    line("Comparisons", 13, True, 18)
    line(f"Annual Percentage Rate (APR)                 {s['apr']:.3f} %", 10, dy=40)
    c.setFont("Helvetica-Oblique", 8)
    c.drawString(50, 40, "SYNTHETIC SAMPLE for software testing only - not a real loan offer. Layout loosely modeled on the CFPB Loan Estimate.")
    c.save()

    truth = dict(id=s["id"], lender=s["lender"], expected=dict(
        loanAmount=s["amount"], interestRate=s["rate"], loanTermYears=s["years"], monthlyPrincipalInterest=printed_pi,
        apr=s["apr"], originationCharges=a_total, totalClosingCosts=closing, prepaymentPenalty=s["prepay"],
        balloonPayment=s["balloon"], rateLocked=s["locked"]), expectedFlags=s["flags"],
        note=("Printed monthly P&I is deliberately wrong by $%d (true value %.2f)" % (s["typo"], true_pi)) if s["typo"] else "")
    json.dump(truth, open(os.path.join(OUT, s["id"] + ".json"), "w"), indent=2)
    print(s["id"], "P&I", true_pi, "printed", printed_pi, "A", a_total, "closing", closing, "A%", round(a_total / s["amount"] * 100, 2), "APRgap", round(s["apr"] - s["rate"], 3))

for s in SAMPLES: build(s)
