-- =====================================================
-- V28: Tools für Kunden, BOM und Knowledge Module
-- =====================================================

-- CEO: search_customers, search_knowledge (Power-Tools)
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"search_customers"'::jsonb
    UNION ALL SELECT '"search_knowledge"'::jsonb
  ) sub
) WHERE role = 'ceo';

-- Support Lead: list_customers, get_customer_detail, search_customers
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"list_customers"'::jsonb
    UNION ALL SELECT '"get_customer_detail"'::jsonb
    UNION ALL SELECT '"search_customers"'::jsonb
  ) sub
) WHERE role = 'support_lead';

-- Production Lead: list_parts, get_bom_tree, get_calculation, get_process_plan
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"list_parts"'::jsonb
    UNION ALL SELECT '"get_bom_tree"'::jsonb
    UNION ALL SELECT '"get_calculation"'::jsonb
    UNION ALL SELECT '"get_process_plan"'::jsonb
  ) sub
) WHERE role = 'production_lead';

-- Knowledge Lead: search_knowledge, get_knowledge_article
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"search_knowledge"'::jsonb
    UNION ALL SELECT '"get_knowledge_article"'::jsonb
  ) sub
) WHERE role = 'knowledge_lead';
